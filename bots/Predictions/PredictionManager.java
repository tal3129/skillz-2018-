package bots.Predictions;


import bots.Data.BotData;
import bots.Types.UpgradedPirate;
import pirates.PirateGame;

import java.util.ArrayList;
import java.util.List;

public class PredictionManager {
    private static PredictionManager instance = null;
    private List<Prediction> predictions;

    private PredictionManager() {
        predictions = new ArrayList<>();
        predictions.add(new PushTowardsMothershipPrediction());
    }

    public void makePredictions() {
        for (Prediction prediction : predictions) {
            PirateGame game = BotData.getInstance().game;

            prediction.predict();

        }
    }

    public static PredictionManager getInstance() {
        if (instance == null)
            instance = new PredictionManager();
        return instance;
    }
    public void updateReliability()
    {
        List<UpgradedPirate>upgradedPirates = BotData.getInstance().upgradedEnemies;
        for(Prediction prediction:predictions)
            prediction.update(getUpgradedPirateByPredictionId(BotData.getInstance().upgradedEnemies,prediction.id));

    }
    private List<UpgradedPirate> getUpgradedPirateByPredictionId(List<UpgradedPirate>upgradedPirates, int predictionId)
    {
        List<UpgradedPirate>res = new ArrayList<>();
        for(UpgradedPirate upgradedPirate:upgradedPirates)
            if(upgradedPirate.getPredictionId()==predictionId)
                res.add(upgradedPirate);
        return res;
    }

    @Override
    public String toString() {
        return "PredictionManager{" +
                "predictions=" + predictions +
                '}';
    }
}
