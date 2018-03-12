package bots.goodOlBot.Predictions;

import bots.goodOlBot.Data.BotData;
import bots.goodOlBot.MapMethods;
import pirates.Location;
import pirates.Mothership;
import pirates.Pirate;
import pirates.PirateGame;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PushTowardsMothershipPrediction extends Prediction {
    @Override
    public void predict() {
        List<Pirate> carryBoys = MapMethods.getCarryBoys(BotData.getInstance().game.getEnemyCapsules());
        if (carryBoys.isEmpty()) {
            return;
        }
        for (Pirate pirate : carryBoys) {
            Location loc = nextLocationWithOffsidePush(pirate);
            if (loc != null) {
                System.out.println("I predict pushing offside!");
                addPrediction(pirate, loc);
            } else {
                System.out.println("I don't predict offside");
            }
        }
    }

    private static Location nextLocationWithOffsidePush(Pirate enemy) {
        if (enemy == null)
            return null;
        BotData data = BotData.getInstance();
        PirateGame game = data.game;
        Mothership closeMs = MapMethods.closest(enemy, game.getEnemyMotherships());

        List<Pirate> possiblePushers = Arrays.stream(game.getEnemyLivingPirates()).filter(pirate -> pirate.canPush(enemy)).collect(Collectors.toList());

        for (Pirate friend : possiblePushers) {
            Location locationAfterPush = enemy.location.towards(closeMs, friend.pushDistance + enemy.maxSpeed - 150); //location in 2 turns
            if (data.isOffside(locationAfterPush, false))
                return locationAfterPush;
        }

        return null;

    }
}
