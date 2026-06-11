package org.junction.catenin.engine

import org.junction.catenin.core.*
import org.junction.catenin.model.objects.GameObject
import kotlin.test.*

class SimpleTargetResolverTest {
    
    @Test
    fun testResolveSelf() {
        val resolver = SimpleTargetResolver()
        val world = GameWorld.empty()
        val sourceObj = GameObject("unit1", "unit")
        
        val targets = resolver.resolveTargets("self", sourceObj, world)
        
        assertEquals(1, targets.size)
        assertEquals(sourceObj, targets[0])
    }
    
    @Test
    fun testResolveAll() {
        val resolver = SimpleTargetResolver()
        
        val unit1 = GameObject("unit1", "unit")
        val unit2 = GameObject("unit2", "unit")
        val building = GameObject("building1", "building")
        
        var world = GameWorld.empty()
        world = world.applyUpdate(AddObjectUpdate(unit1))
        world = world.applyUpdate(AddObjectUpdate(unit2))
        world = world.applyUpdate(AddObjectUpdate(building))
        
        val targets = resolver.resolveTargets("all", unit1, world)
        
        assertEquals(3, targets.size)
        assertTrue(targets.contains(unit1))
        assertTrue(targets.contains(unit2))
        assertTrue(targets.contains(building))
    }
    
    @Test
    fun testResolveSpecificObjectId() {
        val resolver = SimpleTargetResolver()
        
        val unit1 = GameObject("unit1", "unit")
        val unit2 = GameObject("unit2", "unit")
        
        var world = GameWorld.empty()
        world = world.applyUpdate(AddObjectUpdate(unit1))
        world = world.applyUpdate(AddObjectUpdate(unit2))
        
        val targets = resolver.resolveTargets("unit2", unit1, world)
        
        assertEquals(1, targets.size)
        assertEquals(unit2, targets[0])
    }
    
    @Test
    fun testResolveByType() {
        val resolver = SimpleTargetResolver()
        
        val unit1 = GameObject("unit1", "unit")
        val unit2 = GameObject("unit2", "unit")
        val building = GameObject("building1", "building")
        
        var world = GameWorld.empty()
        world = world.applyUpdate(AddObjectUpdate(unit1))
        world = world.applyUpdate(AddObjectUpdate(unit2))
        world = world.applyUpdate(AddObjectUpdate(building))
        
        val targets = resolver.resolveTargets("type:unit", unit1, world)
        
        assertEquals(2, targets.size)
        assertTrue(targets.contains(unit1))
        assertTrue(targets.contains(unit2))
        assertFalse(targets.contains(building))
    }
    
    @Test
    fun testResolveUnknownTarget() {
        val resolver = SimpleTargetResolver()
        val world = GameWorld.empty()
        val sourceObj = GameObject("unit1", "unit")
        
        // Unknown target spec
        val targets = resolver.resolveTargets("unknown", sourceObj, world)
        assertEquals(0, targets.size)
        
        // Non-existent object ID
        val targets2 = resolver.resolveTargets("unit99", sourceObj, world)
        assertEquals(0, targets2.size)
    }
}