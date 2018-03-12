package bots.goodOlBot;

import bots.goodOlBot.Actions.Sails;
import bots.goodOlBot.Data.BotData;
import pirates.Location;
import pirates.Mothership;
import pirates.Pirate;
import pirates.PirateGame;

public class SnakeMovements {

    public static Location snakeLeader(Pirate leader, boolean inScoringPush, Location unitMine) // !!!!!!!!!!called after tryPush!!!!!!!!!!!!!!!!
    {

        BotData data = BotData.getInstance();
        PirateGame game = data.game;

        Mothership closestMs = MapMethods.closest(leader, game.getMyMotherships());


        if (inScoringPush || leader.location.towards(closestMs.location, leader.maxSpeed).distance(closestMs) < closestMs.unloadRange) //
        { //if we can score or in scoring push
            return closestMs.location;
        }

        if (leader.capsule == null) //
        {
            return unitMine;
        }

        return Sails.getCarefulSailLocation(leader, closestMs, leader.maxSpeed);
    }


}
