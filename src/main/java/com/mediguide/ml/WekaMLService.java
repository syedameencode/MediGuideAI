package com.mediguide.ml;

import com.mediguide.models.ModelEvaluationResult;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ArffLoader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WekaMLService — Core Machine Learning pipeline for MediGuide AI.
 *
 * Implements a genuine Weka-based ML pipeline with:
 * - Dataset loading and validation
 * - Model training (Decision Tree, Random Forest, Naive Bayes)
 * - Model evaluation with real metrics
 * - Model persistence (save/load trained models)
 * - Patient-specific prediction
 *
 * All metrics displayed are from actual model evaluation — nothing is hardcoded.
 */
public class WekaMLService {
    private static final Logger LOGGER = Logger.getLogger(WekaMLService.class.getName());
    private static final String MODELS_DIR = "ml_models";

    // Trained classifiers per condition
    private final Map<String, Map<String, Classifier>> trainedModels = new HashMap<>();
    // Evaluation results per condition
    private final Map<String, List<ModelEvaluationResult>> evaluationResults = new HashMap<>();
    // Best model per condition
    private final Map<String, String> bestAlgorithm = new HashMap<>();
    // Loaded datasets
    private final Map<String, Instances> datasets = new HashMap<>();

    private boolean initialized = false;
    private String statusMessage = "Not initialized";

    private static WekaMLService instance;

    public static synchronized WekaMLService getInstance() {
        if (instance == null) {
            instance = new WekaMLService();
        }
        return instance;
    }

    private WekaMLService() {}

    /**
     * Initialize the ML pipeline: generate datasets if missing, load or train models.
     */
    public void initialize() {
        try {
            LOGGER.info("Initializing Weka ML pipeline...");

            // 1. Ensure synthetic datasets exist
            DatasetGenerator.ensureDatasetsExist();

            // 2. Ensure models directory exists
            Path modelsDir = Paths.get(MODELS_DIR);
            if (!Files.exists(modelsDir)) {
                Files.createDirectories(modelsDir);
            }

            // 3. For each condition, load or train models
            String[] conditions = {"Diabetes Mellitus", "Cardiovascular Disease", "Chronic Kidney Disease"};
            for (String condition : conditions) {
                try {
                    loadOrTrainModels(condition);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Could not initialize ML for: " + condition, e);
                }
            }

            initialized = true;
            statusMessage = "ML Engine active — " + trainedModels.size() + " condition(s) loaded";
            LOGGER.info("Weka ML pipeline initialized successfully.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize ML pipeline", e);
            statusMessage = "ML initialization failed: " + e.getMessage();
        }
    }

    /**
     * Load saved models from disk, or train new ones from the dataset.
     */
    private void loadOrTrainModels(String condition) throws Exception {
        String datasetPath = DatasetGenerator.getDatasetPath(condition);
        if (datasetPath == null || !new File(datasetPath).exists()) {
            LOGGER.warning("No dataset found for: " + condition);
            return;
        }

        // Try to load pre-trained models
        String sanitizedName = condition.replaceAll("[^a-zA-Z]", "_").toLowerCase();
        boolean allLoaded = true;
        Map<String, Classifier> condModels = new HashMap<>();

        for (String algo : new String[]{"J48", "RandomForest", "NaiveBayes"}) {
            String modelFile = MODELS_DIR + "/" + sanitizedName + "_" + algo + ".model";
            if (new File(modelFile).exists()) {
                try {
                    Classifier clf = (Classifier) SerializationHelper.read(modelFile);
                    condModels.put(algo, clf);
                    LOGGER.info("Loaded saved model: " + modelFile);
                } catch (Exception e) {
                    LOGGER.warning("Failed to load model " + modelFile + ", will retrain");
                    allLoaded = false;
                }
            } else {
                allLoaded = false;
            }
        }

        // If not all models loaded, train from scratch
        if (!allLoaded || condModels.size() < 3) {
            trainModels(condition);
        } else {
            trainedModels.put(condition, condModels);
            // Evaluate loaded models to populate metrics
            evaluateModels(condition);
        }
    }

    /**
     * Train Decision Tree (J48), Random Forest, and Naive Bayes on the condition's dataset.
     */
    public void trainModels(String condition) throws Exception {
        String datasetPath = DatasetGenerator.getDatasetPath(condition);
        if (datasetPath == null) throw new Exception("No dataset defined for: " + condition);

        File file = new File(datasetPath);
        if (!file.exists()) throw new Exception("Dataset file not found: " + datasetPath);

        // Load dataset
        ArffLoader loader = new ArffLoader();
        loader.setFile(file);
        Instances data = loader.getDataSet();
        data.setClassIndex(data.numAttributes() - 1); // Last attribute is the class (risk)
        datasets.put(condition, data);

        LOGGER.info("Loaded dataset for " + condition + ": " + data.numInstances() + " instances, " + data.numAttributes() + " attributes");

        // Train each algorithm
        Map<String, Classifier> condModels = new HashMap<>();

        // 1. Decision Tree (J48)
        J48 j48 = new J48();
        j48.setConfidenceFactor(0.25f);
        j48.setMinNumObj(2);
        j48.buildClassifier(data);
        condModels.put("J48", j48);

        // 2. Random Forest
        RandomForest rf = new RandomForest();
        rf.setNumIterations(100);
        rf.setSeed(42);
        rf.buildClassifier(data);
        condModels.put("RandomForest", rf);

        // 3. Naive Bayes
        NaiveBayes nb = new NaiveBayes();
        nb.buildClassifier(data);
        condModels.put("NaiveBayes", nb);

        trainedModels.put(condition, condModels);

        // Save models to disk
        String sanitizedName = condition.replaceAll("[^a-zA-Z]", "_").toLowerCase();
        for (Map.Entry<String, Classifier> entry : condModels.entrySet()) {
            String modelFile = MODELS_DIR + "/" + sanitizedName + "_" + entry.getKey() + ".model";
            SerializationHelper.write(modelFile, entry.getValue());
            LOGGER.info("Saved trained model: " + modelFile);
        }

        // Evaluate models
        evaluateModels(condition);
    }

    /**
     * Evaluate all trained models for a condition using 10-fold cross-validation.
     * Metrics are computed from actual experiments — never hardcoded.
     */
    private void evaluateModels(String condition) throws Exception {
        Instances data = datasets.get(condition);
        if (data == null) {
            String datasetPath = DatasetGenerator.getDatasetPath(condition);
            if (datasetPath != null && new File(datasetPath).exists()) {
                ArffLoader loader = new ArffLoader();
                loader.setFile(new File(datasetPath));
                data = loader.getDataSet();
                data.setClassIndex(data.numAttributes() - 1);
                datasets.put(condition, data);
            }
        }
        if (data == null) return;

        Map<String, Classifier> condModels = trainedModels.get(condition);
        if (condModels == null) return;

        List<ModelEvaluationResult> results = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String now = LocalDateTime.now().format(dtf);

        double bestF1 = -1;
        String bestAlgo = "RandomForest";

        Map<String, String> algoDisplayNames = new LinkedHashMap<>();
        algoDisplayNames.put("J48", "Decision Tree (J48)");
        algoDisplayNames.put("RandomForest", "Random Forest");
        algoDisplayNames.put("NaiveBayes", "Naive Bayes");

        for (Map.Entry<String, String> entry : algoDisplayNames.entrySet()) {
            String algoKey = entry.getKey();
            String displayName = entry.getValue();
            Classifier clf = condModels.get(algoKey);
            if (clf == null) continue;

            try {
                Evaluation eval = new Evaluation(data);
                eval.crossValidateModel(clf, data, 10, new Random(42));

                double accuracy = eval.pctCorrect();
                double precision = eval.weightedPrecision() * 100.0;
                double recall = eval.weightedRecall() * 100.0;
                double f1 = eval.weightedFMeasure() * 100.0;

                ModelEvaluationResult result = new ModelEvaluationResult(
                        displayName, condition, accuracy, precision, recall, f1, now
                );
                results.add(result);

                if (f1 > bestF1) {
                    bestF1 = f1;
                    bestAlgo = algoKey;
                }

                LOGGER.info(String.format("  %s [%s]: Accuracy=%.1f%%, Precision=%.1f%%, Recall=%.1f%%, F1=%.1f%%",
                        displayName, condition, accuracy, precision, recall, f1));

            } catch (Throwable e) {
                LOGGER.log(Level.WARNING, "Error evaluating " + displayName + " for " + condition + ": " + e.getMessage());
            }
        }

        evaluationResults.put(condition, results);
        bestAlgorithm.put(condition, bestAlgo);
    }

    /**
     * Generate a prediction for a patient's medical data.
     * Returns: [predictedClass, probability] or null if ML unavailable.
     */
    public String[] predict(String condition, double age, double bmi, double glucose,
                            int systolicBp, int diastolicBp, double cholesterol, double creatinine,
                            boolean familyHistory, String symptoms) {
        if (!isModelAvailable(condition)) return null;

        try {
            String bestAlgo = this.bestAlgorithm.getOrDefault(condition, "RandomForest");
            Classifier clf = trainedModels.get(condition).get(bestAlgo);
            Instances data = datasets.get(condition);

            if (clf == null || data == null) return null;

            // Create a new instance matching the dataset structure
            Instance inst = new DenseInstance(data.numAttributes());
            inst.setDataset(data);

            // Map features based on condition
            switch (condition) {
                case "Diabetes Mellitus":
                    inst.setValue(0, age);
                    inst.setValue(1, bmi);
                    inst.setValue(2, glucose);
                    inst.setValue(3, systolicBp);
                    inst.setValue(4, cholesterol);
                    inst.setValue(5, creatinine);
                    inst.setValue(6, familyHistory ? "Yes" : "No");
                    inst.setValue(7, symptoms.toLowerCase().contains("thirst") ? "Yes" : "No");
                    inst.setValue(8, symptoms.toLowerCase().contains("urination") ? "Yes" : "No");
                    inst.setValue(9, symptoms.toLowerCase().contains("fatigue") ? "Yes" : "No");
                    break;

                case "Cardiovascular Disease":
                    inst.setValue(0, age);
                    inst.setValue(1, bmi);
                    inst.setValue(2, systolicBp);
                    inst.setValue(3, diastolicBp);
                    inst.setValue(4, cholesterol);
                    inst.setValue(5, glucose);
                    inst.setValue(6, creatinine);
                    inst.setValue(7, familyHistory ? "Yes" : "No");
                    inst.setValue(8, symptoms.toLowerCase().contains("chest") ? "Yes" : "No");
                    inst.setValue(9, symptoms.toLowerCase().contains("breath") || symptoms.toLowerCase().contains("shortness") ? "Yes" : "No");
                    inst.setValue(10, symptoms.toLowerCase().contains("fatigue") ? "Yes" : "No");
                    break;

                case "Chronic Kidney Disease":
                    inst.setValue(0, age);
                    inst.setValue(1, bmi);
                    inst.setValue(2, creatinine);
                    inst.setValue(3, systolicBp);
                    inst.setValue(4, diastolicBp);
                    inst.setValue(5, glucose);
                    inst.setValue(6, cholesterol);
                    inst.setValue(7, familyHistory ? "Yes" : "No");
                    inst.setValue(8, symptoms.toLowerCase().contains("edema") || symptoms.toLowerCase().contains("swelling") ? "Yes" : "No");
                    inst.setValue(9, symptoms.toLowerCase().contains("fatigue") ? "Yes" : "No");
                    inst.setValue(10, symptoms.toLowerCase().contains("puffiness") ? "Yes" : "No");
                    break;

                default:
                    return null;
            }

            // Classify
            double classIndex = clf.classifyInstance(inst);
            String predictedClass = data.classAttribute().value((int) classIndex);

            // Get probability distribution
            double[] dist = clf.distributionForInstance(inst);
            double maxProb = dist[(int) classIndex] * 100.0;

            return new String[]{predictedClass, String.format("%.1f", maxProb)};

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "ML prediction failed for " + condition, e);
            return null;
        }
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    public boolean isInitialized() { return initialized; }
    public String getStatusMessage() { return statusMessage; }

    public boolean isModelAvailable(String condition) {
        return trainedModels.containsKey(condition) && !trainedModels.get(condition).isEmpty();
    }

    public List<ModelEvaluationResult> getEvaluationResults(String condition) {
        return evaluationResults.getOrDefault(condition, Collections.emptyList());
    }

    public List<ModelEvaluationResult> getAllEvaluationResults() {
        List<ModelEvaluationResult> all = new ArrayList<>();
        for (List<ModelEvaluationResult> list : evaluationResults.values()) {
            all.addAll(list);
        }
        return all;
    }

    public String getBestAlgorithm(String condition) {
        Map<String, String> displayNames = new HashMap<>();
        displayNames.put("J48", "Decision Tree (J48)");
        displayNames.put("RandomForest", "Random Forest");
        displayNames.put("NaiveBayes", "Naive Bayes");
        String key = bestAlgorithm.getOrDefault(condition, "RandomForest");
        return displayNames.getOrDefault(key, key);
    }

    public Set<String> getAvailableConditions() {
        return trainedModels.keySet();
    }
}
