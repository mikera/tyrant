//
// Interface to descibe an object capable of containing
// one or more instances of Thing
//
// e.g.
//  Maps
//  Beings with inventories
//  Boxes/bags
//

package mikera.tyrant.engine;


public interface ThingOwner {

	void removeThing(Thing thing);

	Map getMap();
}