package bots.goodOlBot;

import bots.goodOlBot.Actions.Pushings;
import bots.goodOlBot.Actions.Sails;
import bots.goodOlBot.Data.BotData;
import bots.goodOlBot.Data.Flags;
import bots.goodOlBot.Types.Unit;
import bots.goodOlBot.Types.UpgradedPirate;
import pirates.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MyBotCompetitive implements PirateBot {

    public PirateGame game;
    private BotData data;
    private static MyBotCompetitive instance;
    public Wormhole minesPortal;

    @Override
    public void doTurn(PirateGame game) {

        //if this is the first turn, initialize the arrays and vars
        if (game.turn == 1)
            initializeStatics(game);

        updateVariables(game); //also create the FREAKING UNITS

        //WE CREATED THE UNITS NOT THEY CAN DO THEIR FREAKING TURN, YES THEY ACT FIRST
        //orders to units with capsule

        doUnitsTurn();

        //orders to attacking waiting in the capusles creation place
        doAttackingTurn();

        //orders to defense
        if (!data.defendingPirates.isEmpty())
            doDefenseTurn();

        if (!data.minePortalPushers.isEmpty())
            doMinePortalPushersTurn();

        if (!data.basePortalPushers.isEmpty())
            doBasePortalPushersTurn();

        printData();

        endTurn();
    }


    public void printData() {
        int count = 0;
        for (Unit unit : data.units)
            count += unit.pirates.size();

        System.out.println("attackers: " + data.attackingPirates.size());
        System.out.println("defenders: " + data.defendingPirates.size());
         System.out.println("in unit: " + count);
         System.out.println("mine portal pushers: " + data.minePortalPushers.size());
         System.out.println("base portal pushers: " + data.basePortalPushers.size());
         System.out.println("total: " + game.getMyLivingPirates().length + ", in roles: " + (count + data.attackingPirates.size() + data.defendingPirates.size() + data.minePortalPushers.size() + data.basePortalPushers.size()));
        System.out.println(data.predictionManager);

        /*
        try {
            checkGameResults();
        } catch (WeDontLeadException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        */
    }


    //the defence is in 2 lines, 2 pirates each. the defence only pushes the capsule or an asteroid.
    private void doDefenseTurn() { //do turn for defensive units
        Mothership enemyMothership = game.getEnemyMotherships()[0];
        List<Pirate> removers = new ArrayList<>();
        // take all the
        List<Capsule> sortedByDistanceList = MapMethods.toSortedByDistanceList(enemyMothership, game.getEnemyCapsules());
        List<Pirate> enemyCarryBois = sortedByDistanceList.stream().filter(capsule -> capsule.holder != null).map(capsule -> capsule.holder).collect(Collectors.toList());
        boolean goToBase = false; //if we can stop an offside by being smart and going our base then go to our base (for defenders that have nothing to do)

        for (int i = 0; i < enemyCarryBois.size(); i++) {
            Pirate carryB = enemyCarryBois.get(i);
            if (i == 0 || carryB.distance(enemyMothership) < data.BIG_CHASE_R) {
                if (data.isOffside(carryB, false) || carryB.stickyBombs.length > 0) {
                    enemyCarryBois.remove(carryB);
                    if (!MapMethods.canGoStraightAgainstSmartDefenseCareFully(carryB, game.getMyLivingPirates(), enemyMothership, 0))
                        goToBase = true;
                }
            }
        }

        Wormhole closeHole = null;

        Pirate carryBoy;
        if (game.getEnemyMotherships().length > 0) {
            closeHole = MapMethods.closest(enemyMothership, game.getAllWormholes());
            if (closeHole != null && (closeHole.distance(enemyMothership) > data.BIG_CHASE_R && closestCapsuleEnemyInMotherRange(game, data.BIG_CHASE_R) == null))
                closeHole = null; //the hole isn't dangerous or we have something more important to do so fuk it
        }

        for (Pirate pirate : data.defendingPirates) { //for each defensive pirate - - (top 10 lines wrote before disaster)
            int i = data.defendingPirates.indexOf(pirate);
            //the 2 outer pirates  should be first on the list
            boolean outerRing = i >= data.defendingPirates.size() / 2;
            Pirate closeEnemy = closestCapsuleEnemyInMotherRange(game, outerRing ? data.BIG_CHASE_R : data.SMALL_CHASE_R);
            if (!(Pushings.defenseTryPush(pirate))) {//if i can get rid of enemy carry-boy or an asteroid get rid of him, else...
                if (closeEnemy != null)  //if in radius chase him
                    pirate.sail(Sails.getNormalCarefulSailLoc(pirate, data.upgraded(closeEnemy).getNextLocation()));
                else {
                    //if there is a portal that needs to be removed and this pirate has nothing to do, and there are not enough removers, send him to remove it
                    if (closeHole != null && removers.size() < (closeHole.distance(enemyMothership) < data.SMALL_CHASE_R ? 2 : 1) /*data.PORTAL_DEFENDING_PUSHER_AMOUNT*/)
                        removers.add(pirate);
                    else { //go towards the closest enemy capsule, but in the radius of the pirate's line
                        Location dest;

                        if (enemyCarryBois.size() > 1) {
                            carryBoy = enemyCarryBois.get(outerRing ? 0 : 0); //change to 0 because we cant really stop 2 directioned attack
                            dest = (MapMethods.getShorterRoute(enemyMothership.getLocation().towards((data.upgraded(carryBoy).getNextLocation()), outerRing ? data.BIG_RADIUS : data.SMALL_RADIUS), pirate));
                        } else if (enemyCarryBois.size() > 0) {
                            carryBoy = enemyCarryBois.get(0);
                            dest = (MapMethods.getShorterRoute(enemyMothership.getLocation().towards(data.upgraded(carryBoy).getNextLocation(), outerRing ? data.BIG_RADIUS : data.SMALL_RADIUS), pirate));
                        } else
                            dest = MapMethods.getShorterRoute(enemyMothership.getLocation().towards(MapMethods.closest(enemyMothership, data.enemyMines), outerRing ? data.BIG_RADIUS : data.SMALL_RADIUS), pirate);

                        if (goToBase) {
                            dest = enemyMothership.location.towards(MapMethods.closest(enemyMothership, MapMethods.getAllEnemyCapsulesCarriers()), enemyMothership.unloadRange);
                        }

                        dest = MapMethods.astroDodge(pirate, dest);
                        pirate.sail(Sails.getNormalCarefulSailLoc(pirate, dest));
                    }
                }
            }
        }
        if (removers.size() > 0)
            removeHole(closeHole, removers);
    }

    //sends 2 defending pirates to remove the hole
    private void removeHole(Wormhole closeHole, List<Pirate> removers) {
        data.portalRemovers = MapMethods.closestToObjectList(closeHole, removers, removers.size());
        for (Pirate pirate : data.portalRemovers) {
            data.defendingPirates.remove(pirate);
            if (pirate.canPush(closeHole))
                Pushings.push(pirate, closeHole, MapMethods.closest(closeHole, data.myMines));
            else {
                Location astroCareLoc = Sails.getNormalCarefulSailLoc(pirate, closeHole.location.towards(pirate, pirate.pushRange));
                pirate.sail(astroCareLoc == null ? closeHole : astroCareLoc);
            }
        }
    }

    //sends 2 defending pirates to remove the hole
    private void doMinePortalPushersTurn() {
        //    MapMethods.getAvarageLoc(data.myMines)
        Location avgOfMyMines = data.myMines[0];
        minesPortal = MapMethods.closest(avgOfMyMines, game.getAllWormholes());
        boolean portalInRightPlace = minesPortal.distance(avgOfMyMines) < data.PORTAL_CLOSE_ENOUGH_TO_TARGET_DISTANCE;
        if (portalInRightPlace)
            data.minesPortalInRightPlaceCount++;
        else
            data.minesPortalInRightPlaceCount = 0;

        Location locAfterPushes = minesPortal.location;

        for (Pirate pirate : data.minePortalPushers) {
            if (!Pushings.tryPush(pirate)) {
                if (pirate.canPush(minesPortal) && !portalInRightPlace) {// if the portal isn't in the right place, go push it there
                    Pushings.push(pirate, minesPortal, avgOfMyMines);
                    locAfterPushes = locAfterPushes.towards(avgOfMyMines, pirate.pushDistance);
                    portalInRightPlace = locAfterPushes.distance(avgOfMyMines) < data.PORTAL_CLOSE_ENOUGH_TO_TARGET_DISTANCE;
                } else {
                    Location astroCareLoc = Sails.getNormalCarefulSailLoc(pirate, MapMethods.getShorterRoute(minesPortal.location.towards(pirate, pirate.pushRange), pirate));
                    pirate.sail(astroCareLoc == null ? minesPortal : astroCareLoc);

                }
            }
        }


    }

    private void doBasePortalPushersTurn() {

        if(minesPortal == null)
        {
            Location avgOfMyMines = data.myMines[0];
            minesPortal = MapMethods.closest(avgOfMyMines, game.getAllWormholes());
        }

        Wormhole closeHole = minesPortal.partner;
        Mothership dest = MapMethods.closest(closeHole, game.getMyMotherships());

        boolean portalInRightPlace = closeHole.distance(dest) < data.PORTAL_CLOSE_ENOUGH_TO_TARGET_DISTANCE;

        if (portalInRightPlace)
            data.basePortalInRightPlaceCount++;
        else
            data.basePortalInRightPlaceCount = 0;

        for (Pirate pirate : data.basePortalPushers) {
            if (!Pushings.tryPush(pirate)) {
                if (pirate.canPush(closeHole) && !portalInRightPlace) // if the portal isn't in the right place, go push it there
                    Pushings.push(pirate, closeHole, dest.location);
                else {
                    Location astroCareLoc = Sails.getNormalCarefulSailLoc(pirate, MapMethods.getShorterRoute(closeHole.location.towards(pirate, pirate.pushRange), pirate));
                    pirate.sail(astroCareLoc == null ? closeHole : astroCareLoc);
                }
            }
        }
    }

    private void doUnitsTurn() {
        for (Unit unit : data.units) /* -> */ doUnitTurn(unit);
    }

    private void doUnitTurn(Unit unit) {
        if (!unit.tryPush()) { //try to push, if we can then that's what we do this turn.
            if (unit.getLeader() != null) {
                if (Flags.ASTEROID_ON_BASE)
                    //sail to base but not into it because asteroid
                    unit.carefulSail(MapMethods.getShorterRoute
                            (data.astroOnOurBase.location
                                    .towards(unit.getLeader().location, game.asteroidSize + 10), unit.getLeader()));

                else
                    unit.carefulSail(MapMethods.getShorterRoute(MapMethods.closest(unit.getLeader(), game.getMyMotherships()), unit.getLeader())); //sailing to closest mother ship using short route
            }
        }
    }
    //Todo not exactly working
    private void doAttackingTurn() {
        //find the best attacking pirate to hit portal
        List<Pirate> attackingDidntSail = new ArrayList<>(data.attackingPirates);

        for (Pirate pirate : data.attackingPirates)
            if (Pushings.tryPush(pirate))
                attackingDidntSail.remove(pirate); //pirate has pushed

        if (attackingDidntSail.size() == 0)
            return;

        int closestAtteckerToMineDistance; //minimal distance between an attacker and a mine
        Pirate closestAtteckerToMine; //the pirate with that minimal distance

        //run until we drained the "ones that didnt sail" pirates list
        while (!attackingDidntSail.isEmpty()) {     //minimum and closest set to the first in the list
            closestAtteckerToMineDistance = attackingDidntSail.get(0).distance(MapMethods.closest(attackingDidntSail.get(0), data.myMines));
            closestAtteckerToMine = attackingDidntSail.get(0);

            //finds the actual minimum distance and pirate with that distance
            for (Pirate pirate : attackingDidntSail) {
                if (data.getUnchasedMines().size() == 0) {
                    if (pirate.distance(MapMethods.closest(pirate, data.myMines)) < closestAtteckerToMineDistance) {
                        closestAtteckerToMineDistance = pirate.distance(MapMethods.closest(pirate, data.myMines));
                        closestAtteckerToMine = pirate;
                    }
                } else if (pirate.distance(MapMethods.closest(pirate, data.getUnchasedMines())) < closestAtteckerToMineDistance) {
                    closestAtteckerToMineDistance = pirate.distance(MapMethods.closest(pirate, data.myMines));
                    closestAtteckerToMine = pirate;
                }
            }

            //remove the closest from the list and let him sail

            closestAtteckerToMine.sail(MapMethods.getShorterRoute(Sails.getNormalCarefulSailLoc(closestAtteckerToMine, MapMethods.closest(closestAtteckerToMine, data.myMines), closestAtteckerToMine.maxSpeed),closestAtteckerToMine));
            data.amountOfAttackersGoingToCapsule[data.indexOf(data.myMines, MapMethods.closest(closestAtteckerToMine, data.myMines))]++;
            attackingDidntSail.remove(closestAtteckerToMine);
        }

    }


    public Pirate closestCapsuleEnemyInMotherRange(PirateGame game, int range) {
        List<Pirate> sortedPirates = MapMethods.toSortedByDistanceList(game.getEnemyMotherships()[0], game.getEnemyLivingPirates());
        for (Pirate ep : sortedPirates) {
            if (ep.capsule != null && data.upgraded(ep).getNextLocation().inRange(game.getEnemyMotherships()[0], range))
                return ep;
        }
        return null;
    }

    //**CALLED ONCE**  initializes the statics, must receive game since this is called before setting the game property
    public void initializeStatics(PirateGame game) {
        this.game = game;
        MapMethods.initializeStatics(game);
        data = BotData.getInstance();
    }

    // called every turn, updates the variables
    public void updateVariables(PirateGame game) {
        this.game = game;
        MapMethods.setGame(game);
        data.update(game);
    }

    public static MyBotCompetitive getInstance() {
        if (instance == null)
            instance = new MyBotCompetitive();
        return instance;
    }

    private Wormhole partnerDest(Pirate attacker) {
        List<Wormhole> partners = new ArrayList<>();
        for (Mothership mothership : mothershipsWithPortal())
            partners.add(MapMethods.closest(mothership, game.getAllWormholes()).partner);
        return MapMethods.closest(attacker, partners);
    }


    public List<Mothership> mothershipsWithPortal() {
        List<Mothership> res = new ArrayList<>();
        for (Mothership mothership : game.getMyMotherships()) {
            Wormhole closeHole = MapMethods.closest(mothership, game.getAllWormholes());
            if (closeHole != null &&
                    (closeHole.distance(mothership) < game.mothershipUnloadRange || data.isOffside(closeHole, true)))
                res.add(mothership);
        }
        return res;
    }

    private void endTurn() {
        for (UpgradedPirate upgradedPirate : data.upgradedEnemies)
            upgradedPirate.setLastLocation(upgradedPirate.pirate.location);
        for (UpgradedPirate upgradedPirate : data.upgradedPirates)
            upgradedPirate.setLastLocation(upgradedPirate.pirate.location);


        int maxStaysOfThePirateThatStayedInTheSamePlaceTheMost = -1; //max of the maximal staying value of each enemy
        int anotherRaduisMaxStays = -1; //the same as max stays but for another enemyRadius
        int maxIndx = 0; //index of the pirate that stayed in the same place the most
        int secondMaxIndx = 0; //index of the pirate that stayed in the same place the most but has another enemyRadius


        List<UpgradedPirate> upgradedEnemies = data.upgradedEnemies;

        for (int i = 0; i < upgradedEnemies.size(); i++) {
            UpgradedPirate current = upgradedEnemies.get(i);
            int m = data.max(current.assumedRadiuses); //the place where this pirate stayed the most
            //int indxOfMaxForPirate = current.assumedRadiuses.indexOf(m);

            if (m > maxStaysOfThePirateThatStayedInTheSamePlaceTheMost) {

                maxStaysOfThePirateThatStayedInTheSamePlaceTheMost = m;

                maxIndx = i;
            }
        }

        int placeInWhichSomeoneStayedTheMost = upgradedEnemies.get(maxIndx).assumedRadiuses.indexOf(maxStaysOfThePirateThatStayedInTheSamePlaceTheMost);

        //find the enemy that stayed in the same place the most but the place where he did it is different than placeInWhichSomeoneStayedTheMost
        for (int i = 0; i < upgradedEnemies.size(); i++) {
            UpgradedPirate current = upgradedEnemies.get(i);
            int m = data.max(current.assumedRadiuses);
            int indxOfMaxForPirate = current.assumedRadiuses.indexOf(m);

            if (m > anotherRaduisMaxStays && indxOfMaxForPirate != placeInWhichSomeoneStayedTheMost) {
                anotherRaduisMaxStays = m;
                secondMaxIndx = i;
            }
        }

        int secondPlaceInWhichSomeoneStayedTheMost = data.upgradedEnemies.get(secondMaxIndx).assumedRadiuses.indexOf(anotherRaduisMaxStays);
        data.enemyRadius = (placeInWhichSomeoneStayedTheMost * UpgradedPirate.UNIT_DISTANCE);
        data.enemySecondRadius = (secondPlaceInWhichSomeoneStayedTheMost * UpgradedPirate.UNIT_DISTANCE);
         System.out.println("rad 1: " + data.enemyRadius);
         System.out.println("rad 2: " + data.enemySecondRadius);


        //changing enemyRadius randomly -start

        if ((game.turn % data.SAME_RADIUS_TURNS) == 0) {
            data.BIG_RADIUS = data.BIG_RADIUS == data.MY_MAX_BIG_RADIUS ? data.MY_MIN_BIG_RADIUS : data.MY_MAX_BIG_RADIUS;
            data.SMALL_RADIUS = data.SMALL_RADIUS == data.MY_MAX_SMALL_RADIUS ? data.MY_MIN_SMALL_RADIUS : data.MY_MAX_SMALL_RADIUS;
        }
        //changing enemyRadius randomly -end

    }
/*
    public void checkGameResults() throws WeDontLeadException {
        if (game.getEnemyCapsules()[0].owner.score >= 7)
            throw new WeDontLeadException();
    }*/
}