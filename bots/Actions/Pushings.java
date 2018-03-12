package bots.Actions;


import bots.Data.BotData;
import bots.Data.Flags;
import bots.MapMethods;
import bots.Types.Unit;
import bots.Types.UpgradedPirate;
import pirates.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * APPROVED!  ~%~  Official seal of Approval by Ido Asraff as an ORGANIZED CLASS.  ~%~
 */

public class Pushings {

    // 0. tries to push a friendly
    // 1. tries to push an enemy capsule
    // 2. tries to push an enemy out
    // 3. tries to push an asteroid
    //for attackers and units only
    public static boolean tryPush(Pirate pirate) {

        BotData data = BotData.getInstance();
        PirateGame game = data.game;

        // if we have an astro on our base, push it towards closest enemy
        if (tryPushAsteroidOutOfOurBase(pirate, game))
            return true;

        //try to push each other, only happens ONCE for a unit
        Unit myUnit = data.getUnit(pirate);

        //if a pirate has the capsule, and he is within push distance to the mothership OR offside - make his unit push each other
        if (tryToPushCarryBoiToScoreOrOffside(myUnit, data, game))
            return true;


        if (tryPushEnemyCapsule(pirate))
            return true;

        List<Integer> friendlyValues = Arrays.stream(game.getMyLivingPirates()).map(MapMethods::superPushValue).collect(Collectors.toList());
        if (!Flags.SUPER_PUSHED && game.getMyself().turnsToSuperPush <= 0 &&
                (friendlyValues.indexOf(data.max(friendlyValues)) == data.indexOf(game.getMyLivingPirates(), pirate) && data.max(friendlyValues) > 0)
                && MapMethods.superPushValue(pirate) >= BotData.MIN_VALUE_FOR_SUPER_PUSH) {
            pirate.superPush();
            Flags.SUPER_PUSHED = true;
            return true;
        }


        // connects the 2 arrays, gets those whose sticky array isn't empty
        for (Pirate stickyHolder : Stream.concat(Arrays.stream(game.getMyLivingPirates()), Arrays.stream(game.getEnemyLivingPirates())).filter(pirate1 -> pirate1.stickyBombs.length > 0 && pirate1.stickyBombs[0].countdown == 0).collect(Collectors.toList()))
            if (tryPushAwayStickyPirate(pirate, stickyHolder))
                return true;

        // try kill
        for (Pirate enemy : game.getEnemyLivingPirates())
            if (data.upgraded(enemy).pushedCount < enemy.numPushesForCapsuleLoss && pirate.stickyBombs.length == 0 && tryKill(pirate, enemy))
                return true;

        // tries to push an asteroid
        if (tryPushAstro(pirate))
            return true;

        if (tryPushFriendlyCapsule(pirate))
            return true;


        return false;
    }

    private static boolean tryPushFriendlyCapsule(Pirate pirate) {
        BotData data = BotData.getInstance();

        for (Pirate carryBoy : Arrays.stream(BotData.game.getMyLivingPirates()).filter(carryBoi -> carryBoi.hasCapsule() && data.upgraded(carryBoi).pushedCount < carryBoi.numPushesForCapsuleLoss - 1).collect(Collectors.toList()))
            if (pirate.canPush(carryBoy)) {
                MapObject closeMs = MapMethods.closest(carryBoy, BotData.game.getMyMotherships());
                Location locAfterPush = Sails.getCarefulSailLocation(carryBoy, closeMs, pirate.pushDistance);

                if (locAfterPush.distance(closeMs) < carryBoy.distance(closeMs)) {
                    pirate.push(carryBoy, locAfterPush);
                    System.out.println("pirate " + pirate + " pushes" + carryBoy + " towards " + MapMethods.closest(carryBoy, BotData.game.getMyMotherships()) + " so he would get to base faster");
                    data.upgraded(carryBoy).pushedCount += 1;

                    return true;
                }
            }
        return false;
    }


    public static boolean tryPushAsteroidOutOfOurBase(Pirate pirate, PirateGame game) {
        Mothership closestMs = MapMethods.closest(pirate, game.getMyMotherships());

        if (Flags.ASTEROID_ON_BASE) {
            for (Asteroid asteroid : game.getLivingAsteroids()) {
                if (asteroid.distance(closestMs) < asteroid.size && asteroid.direction.equals(new Location(0, 0)) && tryPushAstroOnClosestEnemy(pirate, asteroid))
                    return true;
            }
        }

        return false;
    }

    //if a pirate has the capsule, and he is within push distance to the mothership OR offside - make
    // his unit push each other
    public static boolean tryToPushCarryBoiToScoreOrOffside(Unit myUnit, BotData data, PirateGame game) {
        if (myUnit != null) {
            if (myUnit.hasAttackedThisTurn())
                return true;

            Pirate leader = myUnit.getLeader();
            Pirate friend = null;

            if (myUnit.getPirates().size() > 1)
                friend = MapMethods.closest(myUnit.leader, myUnit.getPiratesWithoutLeader()); //carry boi is always first


            // if my pirate has a capsule and we can push him

            if (leader.hasCapsule() && friend != null && friend.canPush(leader)) {

                Mothership closestMs = MapMethods.closest(leader, game.getMyMotherships());
                Location nextLoc = leader.location.towards(closestMs, friend.pushDistance + leader.maxSpeed);

                if ((nextLoc.distance(closestMs) <= closestMs.unloadRange) // i can get pushed straight to mothership
                        ||
                        data.isOffside(nextLoc, true)) {

                    if ((data.getUnit(leader) != null && tryPushMyCapsule(data.getUnit(leader))))
                        return true; //if we can do one of the above and we have a friend then tryPushMyCapsule

                }
            }
        }

        return false;
    }

    // for everyone
    // 1. try to kill a carry boy alone
    // 2. try to kill a carry boy with a friend
    // 3. try to drop a carry boy caps with a friend
    // 4. try tic tac a carry boy
    public static boolean tryPushEnemyCapsule(Pirate pirate) {
        BotData data = BotData.getInstance();

        for (Pirate enemy : data.game.getEnemyLivingPirates()) {
            // if he dosen't have the capsule or we had pushed him enough times to kill him so he's ded, go to the next boi
            if (enemy.capsule == null || data.upgraded(pirate).pushedCount > enemy.numPushesForCapsuleLoss)
                continue;

            if (tryPushEnemyCapsuleForSingleEnemy(pirate, enemy))
                return true;
        }
        //didn't push

        return false;
    }

    public static boolean tryPushAwayStickyPirate(Pirate me, Pirate enemy) {
        BotData data = BotData.getInstance();

        if (me.canPush(enemy)) {
            Location bestLoc = MapMethods.bestPlaceForStickyBombToGoOff(enemy.location, me.pushDistance, data.game.stickyBombExplosionRange, 0);
            System.out.println(me + " pushes " + enemy + " towards" + bestLoc + " so his bomb will explode at a better place");
            me.push(enemy, bestLoc);
            return true;
        }

        return false;
    }

    // receives a carry boi that need to be pushed and a pirate that tries to push him
    private static boolean tryPushEnemyCapsuleForSingleEnemy(Pirate pirate, Pirate enemy) {
        BotData data = BotData.getInstance();
        int threats;
        List<Pirate> possiblePushers;
        possiblePushers = Sails.piratesThatThreatsOnLoc(enemy, data.game.getMyLivingPirates());
        threats = possiblePushers.size();

        // if there is 1 push missing and we have a a super push
        if (threats == enemy.numPushesForCapsuleLoss - 1 && BotData.game.getMyself().turnsToSuperPush == 0)
            threats++;

        Location pushAwayLoc;

        if (tryKill(pirate, enemy)) // try to kill an enemy;
            return true;


        // if there are enough pirates to drop the caps and not enough already pushed
        if (possiblePushers.contains(pirate) && possiblePushers.indexOf(pirate) < enemy.numPushesForCapsuleLoss && threats >= enemy.numPushesForCapsuleLoss) {
            if (data.enemyMines.length != 0) {
                pushAwayLoc = enemy.location.add(enemy.location.subtract(MapMethods.closest(enemy, data.enemyMines))); //todo push away from multiple mines
                pirate.push(enemy, pushAwayLoc);
                updatePush(pirate, pushAwayLoc, enemy);
                data.upgraded(enemy).pushedCount += 1;
                System.out.println("pirate " + pirate + "pushes " + enemy + " towards " + pushAwayLoc + " to remove his caps");
                return true;
            }
        }

        if (tryKillWithSticky(pirate, enemy))
            return true;

        return false;
    }


    // tries to push an asteroid to an enemy's next location if the asteroid isn't pushed already / #OutOfContext / and if the asteroid was not pushed already and also if the asteroid is not pushed by one of our pirates or if one of our pirates pushed it or if it was pushed by by a friendly pirate already or if in the past a certain pushing-action happened that pushed this specific asteroid
    public static boolean tryPushAstro(Pirate pirate) {
        BotData data = BotData.getInstance();

        // tries
        // push an asteroid
        for (Asteroid asteroid : data.game.getLivingAsteroids()) {
            if (!pirate.canPush(asteroid) || data.asteroidsPushedIds.contains(asteroid.id))  // don't push if you can't or it was pushed
                continue;

            if (asteroid.direction.add(asteroid.location).distance(pirate) < asteroid.size + 1) { // if the asteroid will hit me if I stay in my place //change
                //pushes the astroid towards the closest pirate to the closest friendly mothership - to the enemy defenders
                List<Pirate> closestEnemys = MapMethods.toSortedByDistanceList(asteroid, data.game.getEnemyLivingPirates());
                for (Pirate enemy : closestEnemys) {
                    // if the ass will not hit any of my friends next turn if I push it to an enemy, push it
                    if ((Arrays.stream(data.game.getMyLivingPirates()).noneMatch(friend -> (asteroid.location.towards(data.upgraded(enemy).getNextLocation(), pirate.pushDistance).distance(friend.location) < asteroid.size)))) {
                        System.out.println("pirate " + pirate + " pushes " + asteroid + " towards " + enemy + " to kill him");
                        pirate.push(asteroid, data.upgraded(enemy).getNextLocation());
                        updatePush(pirate, data.upgraded(enemy).getNextLocation(), asteroid);
                        // update the astro direction so others won't get stuck in it
                        asteroid.direction = asteroid.location.towards(data.upgraded(enemy).getNextLocation(), pirate.pushDistance).subtract(asteroid.location);
                        data.asteroidsPushedIds.add(asteroid.id); //this asteroid was pushed
                        return true;
                    }
                }

                System.out.println("pirate " + pirate + " pushes " + asteroid + " towards " + asteroid + " to stop it");
                pirate.push(asteroid, asteroid);
                updatePush(pirate, asteroid.getLocation(), asteroid);
                data.asteroidsPushedIds.add(asteroid.id); //this asteroid was pushed
                return true;
            }
        }

        // Didn't push
        return false;
    }

    // push an asteroid towards the closest enemy to it
    public static boolean tryPushAstroOnClosestEnemy(Pirate pirate, Asteroid asteroid) {

        BotData data = BotData.getInstance();

        if (pirate.canPush(asteroid)) {

            MapObject dest = MapMethods.closest(asteroid, data.game.getEnemyLivingPirates());

            if (dest == null) // if the zichron has no pirates, push it to 0,0
                dest = new Location(0, 0);

            pirate.push(asteroid, dest);
            updatePush(pirate, dest.getLocation(), asteroid);
            asteroid.direction = asteroid.location.towards(dest, pirate.pushDistance).subtract(asteroid.location);
            System.out.println(pirate + " pushes " + asteroid + " towards " + dest + " so it won't hit us");

            return true;
        } else
            return false;
    }


    // this method is called on pirates who's push count is below 3
    // for everyone, firstly called while trying to kill the carry boi but then by everyone just to kill enemies, called for each enemy that has pushCount < num_pushes_for_capsule_loss. kills only with one or 2 pirates
    // 1. takes a list of all friendly bois that threaten enemy (i know im there because thats how u call this method) and sort it by pushDistance
    // 2. the first 2 are gonna try to kill the enemy by pushing it out of board or to an asteroid
    // 3. if you can kill the enemy alone kill it and set pushedCount to 10 so no one else will try pushing him
    // 4. if you can't kill the enemy by yourself but you can with 1 friend then: if pushCount is 0, set pushCount to 1 so only your friend will try, if pushCount is 1, set pushCount to 10 because we are done
    // 5. try to kill the enemy with an asteroid
    public static boolean tryKill(Pirate pirate, Pirate enemy) {
        if (tryKillWithAstro(pirate, enemy))
            return true;

        if (tryPushOut(pirate, enemy))
            return true;
        if(tryPushToBlackHole(pirate,enemy))
            return true;

        return false; //cant kill without it
    }

    private static boolean tryPushToBlackHole(Pirate pirate, Pirate enemy) {
        if(!pirate.canPush(enemy))
            return false;
        PirateGame game = BotData.game;
        for(Wormhole wormhole:game.getActiveBlackHoles()){
            if(enemy.distance(wormhole)+enemy.maxSpeed<=wormhole.wormholeRange)
            {
                pirate.push(enemy,wormhole);
                return true;
            }
        }
        return false;
    }

    private static boolean tryKillWithSticky(Pirate pirate, Pirate enemy) {
        BotData data = BotData.getInstance();
        Player myPlayer = data.game.getMyself();
        if (pirate.inStickBombRange(enemy) && myPlayer.turnsToStickyBomb == 0 && enemy.distance(MapMethods.closest(enemy, data.game.getEnemyMotherships())) > data.MIN_DISTANCE_FROM_MS_FOR_STICKING) {
            pirate.stickBomb(enemy);
            System.out.println(pirate + " sticks bomb on " + enemy + " to kill him");
            data.upgraded(enemy).pushedCount = 10; // so others won't push him
            myPlayer.turnsToStickyBomb = data.game.stickyBombReloadTurns; // resets the turns till next sticky, so others won't try to do it
            return true;
        }

        return false;
    }


    private static boolean tryPushOut(Pirate pirate, Pirate enemy) {
        BotData data = BotData.getInstance();
        PirateGame game = data.game;
        int threats;
        List<Pirate> possiblePushers;
        Location deathLoc;

        // get a list of the possible pirate pushers, push with the first 1-2
        if (pirate.canPush(enemy)) {
            possiblePushers = Sails.piratesThatThreatsOnLoc(enemy, game.getMyLivingPirates()); // get all the pirates that threaten the enemy carry boi
            possiblePushers.sort(Comparator.comparing(pirate1 -> pirate1.pushDistance)); // sort the list of possible pushers by their push distance
            threats = possiblePushers.size();

            // if I am looking at the one of the first two possible pushers
            if (possiblePushers.indexOf(pirate) < 2) {
                // try to kill the enemy with 1 pirate

                deathLoc = MapMethods.getDeathLocation(enemy, pirate.pushDistance);

                if (deathLoc != null) { // if I can kill him with 1, kill him
                    pirate.push(enemy, deathLoc);
                    updatePush(pirate, deathLoc, enemy);
                    System.out.println("pirate " + pirate + " pushes " + enemy + " towards " + deathLoc + " can kill alone");
                    data.upgraded(enemy).pushedCount += 10;
                    return true;
                }

                if (threats >= 2) {
                    // try to kill enemy with both pirates
                    int fullPushDistance = possiblePushers.get(0).pushDistance + possiblePushers.get(1).pushDistance;
                    deathLoc = MapMethods.getDeathLocation(enemy, fullPushDistance);
                    if (deathLoc != null) {
                        pirate.push(enemy, deathLoc);
                        updatePush(pirate, deathLoc, enemy);
                        System.out.println("pirate " + pirate + " pushes " + enemy + " towards " + deathLoc + " with a friend");
                        if (data.upgraded(enemy).pushedCount > 0)    // if he was already pushed with one pirate and my push will kill
                            data.upgraded(enemy).pushedCount += 10; //don't push him with more than 2
                        else
                            data.upgraded(enemy).pushedCount = 1;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    //try to push an asteroid on the enemy
    public static boolean tryKillWithAstro(Pirate pirate, Pirate enemy) {

        BotData data = BotData.getInstance();
        PirateGame game = data.game;

        for (Asteroid asteroid : game.getLivingAsteroids()) {
            if (pirate.canPush(asteroid))
                //if after the push the astro will kill the enemy wherever he will go and the astro wasn't pushed already abd the enemy wasn't already pushed
                if (asteroid.location.towards(enemy, pirate.pushDistance).distance(enemy) < asteroid.size - enemy.maxSpeed && !data.asteroidsPushedIds.contains(asteroid.id) && data.upgraded(enemy).pushedCount == 0) {
                    pirate.push(asteroid, enemy);
                    updatePush(pirate, enemy.location, asteroid);
                    asteroid.direction = asteroid.location.towards(enemy, pirate.pushDistance).subtract(asteroid.location);
                    data.upgraded(enemy).pushedCount += 10;
                    System.out.println(pirate + " pushes " + asteroid + " towards " + enemy + " to kill him");
                    data.asteroidsPushedIds.add(asteroid.id);
                    return true;
                }
        }
        return false;
    }


    // functions is called when pirate cannot move
    // ONLY called for the pirate WITH the capsule
    public static boolean tryPushMyCapsule(Unit unit) {
        BotData data = BotData.getInstance();

        if (unit.isFull()) {

            Pirate leader = unit.getLeader();
            Pirate otherPirate = MapMethods.closest(leader, unit.getPiratesWithoutLeader());

            if (otherPirate.canPush(leader) && !unit.attackedThisTurn && data.upgraded(leader).pushedCount < leader.numPushesForCapsuleLoss - 1) { //that's it. we are pushing our friend. we haven't attacked, we can and we should.

                Mothership closeMs = MapMethods.closest(leader, data.game.getMyMotherships());
                Location dest = closeMs.location.towards(leader, closeMs.unloadRange);

                if (Flags.ASTEROID_ON_BASE) //#TOOHARDTOFIX against shitty tactics, we just push close to the mothership. i know this shouldnt be here but i couldnt care less.
                    dest = data.astroOnOurBase.location.towards(unit.getLeader().location, data.astroOnOurBase.size + unit.leader.maxSpeed + 25);


                System.out.println("pushing a carry boi to offside or point...");
                System.out.println("pirate " + otherPirate + " pushes " + leader + " towards " + dest + " to score offside point");
                data.upgraded(leader).pushedCount += 1;
                leader.sail(dest);
                otherPirate.push(leader, dest);
                updatePush(otherPirate, dest, leader);
                unit.attackedThisTurn = true;

                return true;
            }
        }

        return false;
    }


    private static void updatePush(Pirate actor, Location dest, Pirate target) {
        UpgradedPirate upgraded = BotData.getInstance().upgraded(target);
        upgraded.setNextLocation(upgraded.getNextLocation().towards(dest, actor.pushDistance));
    }

    private static void updatePush(Pirate actor, Location dest, SpaceObject target) {
        //do nothing...
        //the method gets the information about a push (that target isn't pirate and update data needed about it. currently we don't need any kind of data about this kind of push, maybe later

    }


    public static void push(Pirate pirate, SpaceObject spaceObject, Location dest) {
        pirate.push(spaceObject, dest);
        if (spaceObject instanceof Pirate)
            updatePush(pirate, dest, (Pirate) spaceObject);
        else
            updatePush(pirate, dest, spaceObject);
    }

    public static int fullPushDistance(List<Pirate> pirates) {
        return pirates.stream().mapToInt(pirate -> pirate.pushDistance).sum();
    }

    public static Pirate pirateCapsuleTowardsAnotherPirateThatCanPushToMS(Pirate pusher, Pirate capsuleCarrier, Mothership ms, List<Pirate> pirates) {
        if (!pusher.canPush(capsuleCarrier))
            return null;
        for (Pirate pirate : pirates)
            if (pirate.distance(ms) < pirate.pushDistance + capsuleCarrier.maxSpeed && pirate.distance(capsuleCarrier) < pusher.pushDistance + capsuleCarrier.maxSpeed)
                return pirate;
        //search for pirate that is next to mothership so it can push to it and we can push to him
        return null;
    }

    public static Location getTicTacLoc(Unit unit, Mothership ms) {
        Pirate capsuleCarrier = unit.getLeader();
        List<Pirate> pushers = new ArrayList<>(unit.pirates);
        pushers.remove(capsuleCarrier);
        Pirate secondPhasePusher = null;

        for (Pirate pirate : pushers)
            if (pirate.stateName.equals("heavy"))
                secondPhasePusher = pirate;
        if (secondPhasePusher == null)
            secondPhasePusher = MapMethods.closest(ms, pushers);

        //serch for closest safe place next to ms
        Location locAfterFirstPhase = Sails.getCarefulSailLocation(capsuleCarrier, ms, capsuleCarrier.pushDistance);
        Location locAfterSecondPhase = locAfterFirstPhase.towards(ms, secondPhasePusher.pushDistance + capsuleCarrier.maxSpeed);


        if (locAfterSecondPhase.distance(ms) <= ms.unloadRange || BotData.getInstance().isOffsideWithForTurns(locAfterSecondPhase, true, BotData.ENEMY_DEFENDERS_FOR_TURNS))
            return locAfterFirstPhase;
        else
            return null;
    }

    public static boolean tryTicTacAttackPush(Unit unit) {
        if (unit.pirates.size() < 3)
            return false;


        BotData data = BotData.getInstance();
        PirateGame game = data.game;
        Pirate capsuleCarrier = unit.getLeader();

        Mothership closeMs = MapMethods.closest(capsuleCarrier, game.getMyMotherships());
        Location pushDest = getTicTacLoc(unit, closeMs);
        if (pushDest == null)
            return false;

        List<Pirate> pushers = new ArrayList<>(unit.pirates);
        pushers.remove(capsuleCarrier);

        Pirate nextTurnPusher = getHeavy(pushers);
        if (nextTurnPusher == null)
            nextTurnPusher = MapMethods.closest(capsuleCarrier, pushers);

        pushers.remove(nextTurnPusher);
        if (capsuleCarrier.canPush(nextTurnPusher) && pushers.get(0).canPush(capsuleCarrier) && nextTurnPusher.pushReloadTurns <= 1) {
            push(capsuleCarrier, nextTurnPusher, pushDest);
            System.out.println("pirate " + capsuleCarrier + " pushes " + nextTurnPusher + " towards " + pushDest + " to ticTac next turn");
            push(pushers.get(0), capsuleCarrier, pushDest);
            System.out.println("pirate " + pushers.get(0) + " pushes " + capsuleCarrier + " towards " + pushDest + " to ticTac next turn");

            return true;

        }
        return false;
    }

    public static boolean isThreatenedByBomb(Pirate pirate) {
        for (StickyBomb stickyBomb : BotData.getInstance().game.getAllStickyBombs())
            if (pirate.distance(stickyBomb) <= stickyBomb.explosionRange + pirate.maxSpeed && stickyBomb.countdown < 3)
                return true;
        return false;
    }

    public static Pirate getHeavy(List<Pirate> pirates) {
        for (Pirate p : pirates)
            if (p.stateName.equals("heavy"))
                return p;
        return pirates.get(0);
    }
}