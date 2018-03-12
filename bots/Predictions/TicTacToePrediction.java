package bots.Predictions;

import bots.Data.BotData;
import bots.Types.UpgradedPirate;
import pirates.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TicTacToePrediction extends Prediction {
        public void predict() {
            PirateGame game = BotData.getInstance().game;
            MapObject dest = predictTicTacToe(game.getEnemyLivingPirates(),game.getEnemyMotherships()[0]);
            if(dest!=null) {
                Mothership mothership = game.getEnemyMotherships()[0];
                Pirate capsuleCarrier = null;

                //taking the closest carry boi and checking about him
                for (Pirate pirate : game.getEnemyLivingPirates())
                    if (pirate.hasCapsule() && (capsuleCarrier == null || pirate.distance(mothership) < capsuleCarrier.distance(mothership)))
                        capsuleCarrier = pirate;
                     addPrediction(capsuleCarrier,dest.getLocation());
                     System.out.println("I predict tic tac!");
            }
        }
        //assuming all enemy pirate don't move - gets all enemies and enemy mothership, and if the enemy can push to start a tic tac chain returns the location he will be in after the first push
        public static MapObject predictTicTacToe(Pirate[] allPirates, Mothership mothership) {

            Pirate capsuleCarrier = null;

            //taking the closest carry boi and checking about him
            for (Pirate pirate : allPirates)
                if (pirate.hasCapsule() && (capsuleCarrier == null || pirate.distance(mothership) < capsuleCarrier.distance(mothership)))
                    capsuleCarrier = pirate;

            if (capsuleCarrier == null)
                return null;

            //copy into pirates all pirates we got except closest carry boi
            Pirate finalCapsule = capsuleCarrier;
            List<Pirate> pirates = Arrays.stream(allPirates).filter(pirate -> pirate != finalCapsule).collect(Collectors.toList());

            //call recursive method
            MapObject res = predictTicTac(new UpgradedPirate(capsuleCarrier), pirates, mothership, 0);
            if(res!=mothership)
                return res;
            return null;
        }

        //gets all pirates without include carry boi  and carry boi and if the enemy can push to start a tic tac chain with this carry boi returns the location he will be in after the first push
        //if mother ship is returned it means he will insert capsule to our base and there is nothing we can do about it
        public static MapObject predictTicTac(UpgradedPirate capsuleCarrier, List<Pirate> allPirates, Mothership mothership, int curTurn) { //curTurn = recursion depth

            //if he can sail to score return the mothership
            if (capsuleCarrier.getLastLocation().distance(mothership.location) <= BotData.getInstance().game.mothershipUnloadRange + capsuleCarrier.pirate.maxSpeed) //reached mothership
                return mothership;

            List<Pirate> pushers = getPushers(capsuleCarrier.getLastLocation(), allPirates, curTurn); //get all friendly pirates that can push carry boi

            if (pushers.isEmpty())
                return null;
            //remove all pushers from allPirates list
            for (Pirate pirate : pushers)
                allPirates.remove(pirate);
            int[] ranges = new int[pushers.size()];

            for (int i = 0; i < ranges.length; i++)
                ranges[i] = pushers.get(i).pushDistance;
            int pushDistance = 0;
            int[] pushDistances = new int[pushers.size()];
            for (int i = 0; i < pushDistances.length; i++) {
                pushDistances[i] = pushers.get(i).pushDistance;
            }
            for (int i = 0; i < capsuleCarrier.pirate.numPushesForCapsuleLoss; i++) {
                int index = getMaxIndex(pushDistances);
                pushDistance += pushers.get(index).pushDistance;
                pushDistances[index] = 0;
            }
            if(capsuleCarrier.getLastLocation().distance(mothership.location)<=pushDistance+ BotData.getInstance().game.mothershipUnloadRange+ BotData.getInstance().game.pirateMaxSpeed) //can be pushed
                return mothership;
            for (int i=0;i<allPirates.size();i++){ //run over each receiving pirate
                Pirate pirate = allPirates.get(i);
                if (capsuleCarrier.getLastLocation().distance(pirate.location) <= pushDistance + BotData.getInstance().game.pirateSize + pirate.maxSpeed) //can be pushed to another receiving pirate
                {
                    Location tmp = capsuleCarrier.getLastLocation(); //saves carry boi location
                    capsuleCarrier.setLastLocation(capsuleCarrier.getLastLocation().towards(pirate.location, pushDistance));
                    if(capsuleCarrier.getLastLocation().distance(pirate.location)<pushDistance + BotData.getInstance().game.pushRange + pirate.maxSpeed)
                        capsuleCarrier.setLastLocation(pirate.location);
                    if (predictTicTac(capsuleCarrier, allPirates, mothership, curTurn + 1) != null)
                        return pirate;
                    capsuleCarrier.setLastLocation(tmp);
                }
            }
            return null;
        }

        //end tictac prediction

        private static List<Pirate> getPushers(Location capsule, List<Pirate> pirates, int curTurn) {
            List<Pirate> pushers = new ArrayList<>();
            for (Pirate pirate : pirates)
                if (futureCanPush(pirate, capsule, curTurn))
                    pushers.add(pirate);
            return pushers;
        }
        public static int getMaxIndex(int[] arr) {
            int maxIndex = 0;
            for (int i = 1; i < arr.length; i++)
                if (arr[i] > arr[maxIndex])
                    maxIndex = i;
            return maxIndex;
        }
        private static boolean futureCanPush(Pirate pirate, Location spaceObject, int curTurn) {
            if (((pirate.location.distance(spaceObject) <= (BotData.getInstance().game.pushRange + BotData.getInstance().game.pirateSize)))) {
                return (pirate.pushReloadTurns <= curTurn);
            } else {
                return false;
            }

        }

    @Override
    public String toString() {
        return "TicTacPrediction{" +
                "reliability=" + reliability +
                '}';
    }
}
