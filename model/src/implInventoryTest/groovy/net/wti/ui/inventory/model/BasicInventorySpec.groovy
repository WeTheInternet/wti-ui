package net.wti.ui.inventory.model

import spock.lang.Specification
import xapi.jre.model.ModelServiceJre
import xapi.model.X_Model
import xapi.model.api.ModelList

class BasicInventorySpec extends Specification {

    void setupSpec() {
        final ModelServiceJre service = X_Model.getService() as ModelServiceJre
        service.register(TestInventoryStack)
        service.register(TestBasicInventory)
        service.getOrMakeModelManifest(TestInventoryStack)
        service.getOrMakeModelManifest(TestBasicInventory)
    }

    def "concrete inventory manifest preserves its exact stack type"() {
        when:
        def manifest = X_Model.getService().findManifest(TestBasicInventory)
        def stacks = manifest.getMethodData("stacks")
        def capacity = manifest.getMethodData("capacity")

        then:
        manifest.type == TestBasicInventory.MODEL_TYPE
        stacks.type == ModelList
        stacks.typeParams as List == [TestInventoryStack]
        manifest.getMethodType("setStacks").name() == "SET"
        capacity.type == Integer.TYPE
        manifest.getMethodType("getCapacity").name() == "GET"
    }

    def "capacity normalizes unlimited values and reports boundedness"() {
        given:
        def inventory = X_Model.create(TestBasicInventory)

        expect:
        inventory.capacity == BasicInventory.UNLIMITED_CAPACITY
        inventory.isUnlimited()
        !inventory.isBounded()

        when:
        inventory.setCapacity(-7)

        then:
        inventory.capacity == BasicInventory.UNLIMITED_CAPACITY
        inventory.isUnlimited()
        !inventory.isBounded()

        when:
        inventory.setCapacity(12)

        then:
        inventory.capacity == 12
        !inventory.isUnlimited()
        inventory.isBounded()
    }

    def "typed stacks survive an XApi serialization round trip"() {
        given:
        def inventory = X_Model.create(TestBasicInventory)
        inventory.key = X_Model.newKey("test", TestBasicInventory.MODEL_TYPE, "pantry")
        inventory.setCapacity(12)

        def berries = X_Model.create(TestInventoryStack)
        berries.key = X_Model.newKey("test", TestInventoryStack.MODEL_TYPE, "berry-stack")
        berries.itemId = "berry"
        berries.count = 5
        inventory.stacks().add(berries)

        when:
        def encoded = X_Model.serialize(TestBasicInventory, inventory)
        def decoded = X_Model.deserialize(TestBasicInventory, encoded)
        def decodedStack = decoded.stacks().iterator().next()

        then:
        decoded.key == inventory.key
        decoded.capacity == 12
        decoded.isBounded()
        decoded.stacks().modelType == TestInventoryStack
        decoded.stacks().size() == 1
        decodedStack instanceof TestInventoryStack
        decodedStack.key == berries.key
        decodedStack.itemId == "berry"
        decodedStack.count == 5
    }
}
