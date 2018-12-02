package bots.goodOlBot.Predictions;

import bots.goodOlBot.Data.BotData;
import bots.goodOlBot.Types.UpgradedPirate;
import pirates.Location;
import pirates.Pirate;

import java.util.List;

/**
 * Represents a prediction of the enemy's moves.
 * This feature wasn't used in the skillz competition because it was hard testing it and didn't affect enough our gameplay to make the hard work affordable.
 */
public abstract class Prediction {
    public static int AUT_NUMBERING=1;
    //the method returns
    /**
     * This field evaluates how much should this prediction be trusted.
     * The field is updated according to the differences each turn between the predicted moves to the real ones.
     */
    protected int reliability;
    protected int id;

    public Prediction() {
        reliability=0;
        id=AUT_NUMBERING;
        AUT_NUMBERING++;
    }

    /**
     * Update the next location of the enemy's pirates according to the specific prediction logic
     */
    public abstract void predict();

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
