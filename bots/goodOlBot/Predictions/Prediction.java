package bots.goodOlBot.Predictions;

import bots.goodOlBot.Data.BotData;
import bots.goodOlBot.Types.UpgradedPirate;
import pirates.Location;
import pirates.Pirate;

import java.util.List;

public abstract class Prediction {
    public static int AUT_NUMBERING=1;
    //the method returns
    protected int reliability;
    protected int id;

    public Prediction() {
        reliability=0;
        id=AUT_NUMBERING;
        AUT_NUMBERING++;
    }

    public abstract void predict();
    // receives a list of pirates that was predicted last turn, and update the reliability of the prediction accordingly


    public void update(List<UpgradedPirate>trueActions) {
        for (UpgradedPirate upgradedPirate : trueActions) {
            if (upgradedPirate.getNextLocation().distance(upgradedPirate.pirate.location) < BotData.getInstance().DEVIATION_LOCATIONS)
                reliability++;
             else
                reliability--;
        }

    }

    public void addPrediction(Pirate pirate, Location location) {
        UpgradedPirate upgradedPirate = BotData.getInstance().upgraded(pirate);
        upgradedPirate.setNextLocation(location);
        upgradedPirate.setCertainty(reliability);
        upgradedPirate.setPredictionId(id);
    }
}