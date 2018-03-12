package bots.Actions;

import bots.Data.BotData;
import bots.MapMethods;
import bots.Types.Unit;
import bots.Types.UpgradedPirate;
import pirates.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * APPROVED!  ~%~  Official seal of Approval by Ido Asraff as an ORGANIZED CLASS.  ~%~
 */
public class Sails {

    //gets a unit and a destination and send the unit towards the destination safely.
    public static void carefulSailOfUnits(Unit unit, MapObject dest) {
        BotData data = BotData.getInstance();
        PirateGame game = data.game;

        //if the leader can reach HIS dest this turn, don't be careful
        if (unit.leader.distance(dest.getLocation().towards(unit.leader, game.mothershipUnloadRange)) < unit.leader.maxSpeed) {
            unit.sail(dest);
            System.out.println("I am going to score a point. Hurray.");
            return;
        }
        if (unit.offside) {
            unit.sail(getCarefulSailLocation(unit.leader, dest, unit.leader.maxSpeed));
            return;
        }

        MapObject leaderDest;
        leaderDest = dest;

        // pushNextTurn  - if we can push our carry boi next turn but only with a heavy friend then switch
        unit.swap2Fat2PushLeader(dest.getLocation());

        Location nextLeaderLoc = getCarefulSailLocation(unit.leader, leaderDest, unit.leader.maxSpeed);

        unit.leader.sail(nextLeaderLoc);

        for (Pirate pirate : unit.getPirates())
            if (pirate != unit.leader)
                pirate.sail(nextLeaderLoc);

    }


    public static Location getCarefulSailLocation(Pirate pirate, MapObject dest, int speed) {

        PirateGame game = BotData.getInstance().game;
        Location l;

        //if i can go to dest normally with a careful distance
        l = getCarefulSailLocationWithThreats(pirate, dest, speed, pirate.numPushesForCapsuleLoss - 1, true);

        if (l != null)
            return l;

        System.out.println("finding a less careful location");
        //if we didnt find a place to go with the careful distance, get rid of that shyte and try again
        for (int i = pirate.numPushesForCapsuleLoss - 1; i < game.getEnemyLivingPirates().length; i++) {
            l = getCarefulSailLocationWithThreats(pirate, dest, speed, i, false);
            if (l != null)
                return l;
        }

        System.out.println("running backwards");
        // if you don't have any careful locations, sail backwards
        Location backDest = getNormalCarefulSailLoc(pirate, dest, -speed);
        if (backDest == null)
            backDest = pirate.location;
        return backDest; //sail back
    }

    private static Location getCarefulSailLocationWithThreats(Pirate pirate, MapObject t, int speed, int minCertainThreats, boolean useCarefulDistance) {

        PirateGame game = BotData.getInstance().game;
        Location dest = t.getLocation();

        //if my current location is already threatened, don't get into another ship's range cuz being in 2 is very bad they can tek capsul
        if (totalThreats(pirate.getLocation(), pirate.getLocation(), game.getEnemyLivingPirates(), 0) > minCertainThreats + 1) {
            System.out.println("Omae Wa Mou Shindeiru");
            return null;
        }

        //normalize to 200 distance
        dest = pirate.getLocation().towards(dest, speed);


        //if i dont get another threat going forward (or get out of a threat) go forward, else...
        if (totalThreats(t.getLocation(), dest, game.getEnemyLivingPirates(), useCarefulDistance ? BotData.getInstance().CAREFUL_DISTANCE : 0) <= minCertainThreats)
            return pirate.getLocation().towards(dest, speed);
        else {
            System.out.println("We are straying of our way");
            Location closest = null;
            for (int deg = 0; (deg < 360); deg += 1) {
                Location l = new Location((int) Math.round(Math.cos(deg) * speed), (int) Math.round((Math.sin(deg) * speed)));
                l = l.add(pirate.getLocation());
                boolean enemyCanKillMe = MapMethods.getDeathLocation(pirate, piratesThatThreatsOnLoc(l, game.getEnemyLivingPirates()).stream().mapToInt(pirate1 -> pirate1.pushDistance).sum()) != null;
                // if this location is threatened by the accepted amount (or less) and isn't a death place (enemy can push me to asteroid out of board) and gets me closer to my location then the routes we checked until now, this is the best route
                if (l.inMap() && !enemyCanKillMe && totalThreats(l, l, game.getEnemyLivingPirates(), useCarefulDistance ? BotData.getInstance().CAREFUL_DISTANCE : 0) <= minCertainThreats
                        && (closest == null || l.distance(dest)  < closest.distance(dest))) {
                    closest = l;
                }
            }

            if (closest != null)
                return pirate.getLocation().towards(closest, speed);

        }
        return null;
    }

    public static Location getNormalCarefulSailLoc(Pirate pirate, MapObject destObj) {
        return getNormalCarefulSailLoc(pirate, destObj, pirate.maxSpeed);
    }


    private static Location getNormalCarefulSailLoc(Pirate pirate, MapObject destObj, int speed) { //NEVER RETURN NULL
        System.out.println(pirate.id+"doing normal c...");
        Location dest = destObj.getLocation();
        dest = pirate.location.towards(dest, speed);
        //normalize to 200 distance
        //checks if no - one threaten me in next step
        if (assThreats(dest, BotData.getInstance().CAREFUL_DISTANCE) == 0 && stickyBombsThreats(dest, BotData.getInstance().CAREFUL_DISTANCE, BotData.getInstance().MAX_BOMB_COUNTDOWN_SAFE) == 0&&portalsWeSouldntPassInOnWay(dest)==0)
            return dest;
        else {
            Location closest = null;
            for (int deg = 0; deg < 360; deg++) {
                Location l = new Location((int) Math.round(Math.cos(deg) * speed), (int) Math.round((Math.sin(deg) * speed)));

                l = l.add(pirate.getLocation());
                if (l.inMap() && assThreats(l, BotData.getInstance().CAREFUL_DISTANCE) == 0 && stickyBombsThreats(dest, BotData.getInstance().CAREFUL_DISTANCE, BotData.getInstance().MAX_BOMB_COUNTDOWN_SAFE) == 0 &&portalsWeSouldntPassInOnWay(dest)==0&& (closest == null || l.distance(dest) < closest.distance(dest))) {
                    closest = l;
                }
            }
            if (closest != null)
                return closest;
        }
        return dest; // if you are already dead, go to your dest
    }


    public static int totalThreats(Location dest, Location toward, Pirate[] prts, int carefulDistance) {
        return piratesThreatsOnLoc(toward, prts, carefulDistance, 1) + assThreats(toward, carefulDistance) + stickyBombsThreats(toward, carefulDistance, BotData.getInstance().MAX_BOMB_COUNTDOWN_SAFE) + portalsWeSouldntPassInOnWay(toward);
    }

    public static int piratesThreatsOnLoc(Location loc, Pirate[] prts, int carefulDistance, int carefulTurns) {
        int c = 0;
        for (Pirate prt : prts)
            //if carefulTurns is 1, I need to see forward one turn. otherwise, I need the amount of threats right now
            //tpdo change back
            if (((carefulTurns == 0 ? prt.pushReloadTurns : prt.pushReloadTurns - BotData.CAREFUL_RELOAD_TURNS) <= 0 && loc.inRange((carefulTurns == 0 ? prt.getLocation() : carefulNextLocation(loc, prt)), prt.pushRange + carefulDistance)))
                c++;

        return c;
    }

    public static int directPiratesThreatsOnLoc(Location loc, Pirate[] prts) {
        int c = 0;
        for (Pirate prt : prts)
            //if carefulTurns is 1, I need to see forward one turn. otherwise, I need the amount of threats right now
            //tpdo change back
            if (prt.pushReloadTurns <= 0 && loc.inRange(prt.getLocation(), prt.pushRange))
                c++;

        return c;
    }

    //returns a list of all piratez from prts[] that threaten this location
    public static List<Pirate> piratesThatThreatsOnLoc(MapObject loc, Pirate[] prts) {
        return piratesThatInRangeAroundLoc(loc, prts, BotData.getInstance().game.pushRange).stream().filter(pirate -> pirate.pushReloadTurns == 0).collect(Collectors.toList());
    }

    public static int assThreats(Location loc, int carefulDistance) {
        PirateGame game = BotData.getInstance().game;
        int c = 0;
        for (Asteroid asteroid : game.getLivingAsteroids()) {
            //asteroid will be at the checked locatoion in the next few turns
            if (isThreatenedByAst(loc, asteroid, 5, 50)) {
                c += 100; // if the location is threatened by an ass don't go there
            }
        }
        return c;
    }

    public static int stickyBombsThreats(Location loc, int carefulDistance, int maxTurnCountDown) {
        int c = 0;
        for (StickyBomb stickyBomb : BotData.getInstance().game.getAllStickyBombs()) {
            if (stickyBomb.distance(loc) <= stickyBomb.explosionRange + carefulDistance && stickyBomb.countdown <= BotData.getInstance().MAX_BOMB_COUNTDOWN_SAFE) //loc is threatened
                c++;
        }
        return c;
    }

    public static boolean isThreatenedByAst(Location loc, Asteroid asteroid, int predictionDepth, int carefulDistance) {
        PirateGame game = BotData.getInstance().game;

        Location[] futureLocations = UpgradedPirate.futureLocations(asteroid.getLocation(), asteroid.direction, predictionDepth);
        return Arrays.stream(futureLocations).anyMatch(location -> loc.inRange(location, game.asteroidSize + carefulDistance));
    }

    public static Location carefulNextLocation(MapObject myPirate, Pirate enemy) { // if he is close to one of the radiuses
        PirateGame game = BotData.getInstance().game;
        //    if (Math.abs(enemy.location.distance(game.getMyMotherships()[0]) - BotData.getInstance().enemyRadius) < 500 || Math.abs(enemy.location.distance(game.getMyMotherships()[0]) - BotData.getInstance().enemySecondRadius) < 500)
        return enemy.location.towards(myPirate, enemy.maxSpeed);
        //    else
        //      return BotData.getInstance().upgraded(enemy).getNextLocation();
    }

    private static Location getCarefulSailLocation(Pirate pirate, MapObject t, Pirate[] enemies, int threatenedByPirateRange, int speed, int minCertainThreats, boolean useCarefulDistance) {
        PirateGame game = BotData.getInstance().game;
        Location dest = t.getLocation();

        //if my current location is already threatened, don't get into another ship's range cuz being in 2 is very bad they can tek capsul
        if (totalThreats(pirate.getLocation(), pirate.getLocation(), game.getEnemyLivingPirates(), 0) > minCertainThreats + 1) {
            return null;
        }

        //normalize to 200 distance
        dest = pirate.getLocation().towards(dest, speed);

        //if i dont get another threat going forward (or get out of a threat) go forward, else...
        if (totalThreats(t.getLocation(), dest, game.getEnemyLivingPirates(), useCarefulDistance ? BotData.getInstance().CAREFUL_DISTANCE : 0) <= minCertainThreats)
            return pirate.getLocation().towards(dest, speed);
        else {
            Location closest = null;
            for (int deg = 0; deg < 360; deg++) {
                Location l = new Location((int) Math.round(Math.cos(deg) * speed), (int) Math.round((Math.sin(deg) * speed)));
                l = l.add(pirate.getLocation());
                boolean enemyCanKillMe = MapMethods.getDeathLocation(pirate, piratesThatThreatsOnLoc(l, game.getEnemyLivingPirates()).stream().mapToInt(pirate1 -> pirate1.pushDistance).sum()) != null;
                // if this location is threatened by the accepted amount (or less) and isn't a death place (enemy can push me to asteroid out of board) and gets me closer to my location then the routes we checked until now, this is the best route
                if (l.inMap() && !enemyCanKillMe && totalThreats(t.getLocation(), l, game.getEnemyLivingPirates(), useCarefulDistance ? BotData.getInstance().CAREFUL_DISTANCE : 0) <= minCertainThreats && (closest == null || l.distance(dest) < closest.distance(dest))) {
                    closest = l;
                }
            }

            if (closest != null)
                return pirate.getLocation().towards(closest, speed);

        }
        return null;
    }

    public static List<Pirate> piratesThatInRangeAroundLoc(MapObject loc, Pirate[] pirates, int range) {
        List<Pirate> res = new ArrayList<>();
        for (Pirate pirate : pirates)
            if (loc.inRange(pirate, range))
                res.add(pirate);
        return res;
    }


    //num of portals that in our at and we don't want them in
    public static int portalsWeSouldntPassInOnWay(Location toward) {
        int c = 0;
        PirateGame game = BotData.game;
        for (Wormhole portal : game.getAllWormholes())
            if (toward.distance(portal) <= portal.wormholeRange+600) { //we will go into portal that we don't want to go to and it's active
                c++;
            }
        if (c > 0)
            System.out.println("portal blocks us");
        return c;

    }
}
