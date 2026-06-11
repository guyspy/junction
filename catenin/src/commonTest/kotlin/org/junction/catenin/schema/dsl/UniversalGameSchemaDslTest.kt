package org.junction.catenin.schema.dsl

import org.junction.catenin.model.definitions.PropertyType
import org.junction.catenin.model.values.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class UniversalGameSchemaDslTest {
    
    @Test
    fun testBasicSchemaDslConstruction() {
        val schema = universalGameSchema {
            meta {
                name = "DSL Test Game"
                targetAge = intArrayOf(8, 12)
                participantCount = intArrayOf(2, 4)
            }
            
            objectTypes {
                objectType("creature") {
                    properties {
                        int("health", initial = 100, min = 0, max = 200)
                        string("name", initial = "Default Creature")
                        bool("alive", initial = true)
                    }
                    states {
                        bool("activated", initial = false)
                        string("position", initial = "none")
                    }
                }
                
                objectType("card") {
                    properties {
                        int("cost", initial = 1, min = 0)
                        int("damage", initial = 1)
                        string("name", initial = "Basic Card")
                    }
                }
            }
        }
        
        // Verify meta
        assertEquals("DSL Test Game", schema.meta.name)
        assertEquals(listOf(8, 12), schema.meta.targetAge.toList())
        assertEquals(listOf(2, 4), schema.meta.participantCount.toList())
        
        // Verify object types
        assertEquals(2, schema.objectTypes.size)
        assertTrue(schema.hasObjectType("creature"))
        assertTrue(schema.hasObjectType("card"))
        
        // Verify creature type
        val creatureType = schema.getObjectType("creature")
        assertNotNull(creatureType)
        
        assertEquals(3, creatureType.properties.size)
        assertTrue(creatureType.hasProperty("health"))
        assertTrue(creatureType.hasProperty("name"))
        assertTrue(creatureType.hasProperty("alive"))
        
        val healthProp = creatureType.getPropertyDefinition("health")
        assertNotNull(healthProp)
        assertEquals(PropertyType.INT, healthProp.type)
        assertEquals(IntValue(100), healthProp.initial)
        assertEquals(IntValue(0), healthProp.min)
        assertEquals(IntValue(200), healthProp.max)
        
        assertEquals(2, creatureType.states.size)
        assertTrue(creatureType.hasState("activated"))
        assertTrue(creatureType.hasState("position"))
        
        // Verify card type
        val cardType = schema.getObjectType("card")
        assertNotNull(cardType)
        assertEquals(3, cardType.properties.size)
        assertTrue(cardType.hasProperty("cost"))
        assertTrue(cardType.hasProperty("damage"))
        assertTrue(cardType.hasProperty("name"))
        
        val costProp = cardType.getPropertyDefinition("cost")
        assertNotNull(costProp)
        assertEquals(PropertyType.INT, costProp.type)
        assertEquals(IntValue(1), costProp.initial)
        assertEquals(IntValue(0), costProp.min)
    }
    
    @Test
    fun testSchemaWithInstances() {
        val schema = universalGameSchema {
            meta {
                name = "Game with Instances"
                targetAge = intArrayOf(10, 16)
                participantCount = intArrayOf(2, 2)
            }
            
            objectTypes {
                objectType("card") {
                    properties {
                        string("name", initial = "Basic Card")
                        int("cost", initial = 1)
                        int("damage", initial = 1)
                    }
                }
            }
            
            instances {
                instance("fire_spell", "card") {
                    properties {
                        "name" to "Fire Spell"
                        "cost" to "3"
                        "damage" to "4"
                    }
                }
                
                instance("ice_bolt", "card") {
                    properties {
                        "name" to "Ice Bolt"
                        "cost" to "2" 
                        "damage" to "3"
                    }
                    states {
                        "frozen" to "true"
                    }
                }
                
                instance("weak_goblin", "creature") {
                    properties {
                        "name" to "Weak Goblin"
                        "health" to "30"
                    }
                }
            }
        }
        
        // Verify instances
        assertEquals(3, schema.instances.size)
        assertTrue(schema.hasInstance("fire_spell"))
        assertTrue(schema.hasInstance("ice_bolt"))
        assertTrue(schema.hasInstance("weak_goblin"))
        
        // Verify fire spell instance
        val fireSpell = schema.getInstance("fire_spell")
        assertNotNull(fireSpell)
        assertEquals("card", fireSpell.objectType)
        assertEquals("Fire Spell", fireSpell.properties["name"])
        assertEquals("3", fireSpell.properties["cost"])
        assertEquals("4", fireSpell.properties["damage"])
        
        // Verify ice bolt instance with states
        val iceBolt = schema.getInstance("ice_bolt")
        assertNotNull(iceBolt)
        assertEquals("card", iceBolt.objectType)
        assertEquals("Ice Bolt", iceBolt.properties["name"])
        assertEquals("true", iceBolt.states["frozen"])
        
        // Verify generic instance creation
        val weakGoblin = schema.getInstance("weak_goblin")
        assertNotNull(weakGoblin)
        assertEquals("creature", weakGoblin.objectType)
        assertEquals("Weak Goblin", weakGoblin.properties["name"])
        assertEquals("30", weakGoblin.properties["health"])
    }
    
    @Test
    fun testPropertyTypes() {
        val schema = universalGameSchema {
            meta {
                name = "Property Types Test"
                targetAge = intArrayOf(8, 12)
                participantCount = intArrayOf(1, 1)
            }
            
            objectTypes {
                objectType("test_object") {
                    properties {
                        int("score", initial = 0, min = 0, max = 1000)
                        string("username", initial = "player")
                        bool("active", initial = true)
                        objectRef("target", initial = "none")
                    }
                }
            }
        }
        
        val testObject = schema.getObjectType("test_object")
        assertNotNull(testObject)
        
        // Verify int property
        val scoreProp = testObject.getPropertyDefinition("score")
        assertNotNull(scoreProp)
        assertEquals(PropertyType.INT, scoreProp.type)
        assertEquals(IntValue(0), scoreProp.initial)
        assertEquals(IntValue(0), scoreProp.min)
        assertEquals(IntValue(1000), scoreProp.max)
        
        // Verify string property
        val usernameProp = testObject.getPropertyDefinition("username")
        assertNotNull(usernameProp)
        assertEquals(PropertyType.STRING, usernameProp.type)
        assertEquals(StringValue("player"), usernameProp.initial)
        
        // Verify bool property
        val activeProp = testObject.getPropertyDefinition("active")
        assertNotNull(activeProp)
        assertEquals(PropertyType.BOOL, activeProp.type)
        assertEquals(BoolValue(true), activeProp.initial)
        
        // Verify object ref property
        val targetProp = testObject.getPropertyDefinition("target")
        assertNotNull(targetProp)
        assertEquals(PropertyType.OBJECT_REF, targetProp.type)
        assertEquals(ObjectRefValue("none"), targetProp.initial)
    }
    
    @Test
    fun testGenericObjectTypeCreation() {
        val schema = universalGameSchema {
            meta {
                name = "Generic Test"
                targetAge = intArrayOf(8, 12)
                participantCount = intArrayOf(2, 4)
            }
            
            objectTypes {
                // Test generic object type creation
                objectType("widget") { }
                objectType("component") { }
                objectType("entity") { }
                objectType("item") { }
            }
            
            instances {
                // Test generic instance creation
                instance("basic_widget", "widget") { }
                instance("red_component", "component") { }
                instance("special_entity", "entity") { }
            }
        }
        
        // Verify generic object types
        assertTrue(schema.hasObjectType("widget"))
        assertTrue(schema.hasObjectType("component"))
        assertTrue(schema.hasObjectType("entity"))
        assertTrue(schema.hasObjectType("item"))
        
        // Verify generic instances
        assertTrue(schema.hasInstance("basic_widget"))
        assertTrue(schema.hasInstance("red_component"))
        assertTrue(schema.hasInstance("special_entity"))
        
        assertEquals("widget", schema.getInstance("basic_widget")?.objectType)
        assertEquals("component", schema.getInstance("red_component")?.objectType)
        assertEquals("entity", schema.getInstance("special_entity")?.objectType)
    }
    
    @Test
    fun testMinimalSchema() {
        val schema = universalGameSchema {
            meta {
                name = "Minimal Game"
                targetAge = intArrayOf(5, 99)
                participantCount = intArrayOf(1, 8)
            }
            
            objectTypes {
                objectType("player") { }
            }
        }
        
        assertEquals("Minimal Game", schema.meta.name)
        assertEquals(1, schema.objectTypes.size)
        assertEquals(0, schema.instances.size)
        assertEquals(0, schema.triggers.size)
    }
    
    @Test
    fun testValidationErrors() {
        // Test missing meta
        assertFailsWith<IllegalStateException> {
            universalGameSchema {
                objectTypes {
                    objectType("player") { }
                }
            }
        }
        
        // Test missing name
        assertFailsWith<IllegalStateException> {
            universalGameSchema {
                meta {
                    targetAge = intArrayOf(8, 12)
                    participantCount = intArrayOf(2, 4)
                }
                objectTypes {
                    objectType("player") { }
                }
            }
        }
        
        // Test missing target age
        assertFailsWith<IllegalStateException> {
            universalGameSchema {
                meta {
                    name = "Test Game"
                    participantCount = intArrayOf(2, 4)
                }
                objectTypes {
                    objectType("player") { }
                }
            }
        }
    }
    
    @Test
    fun testEmptyCollections() {
        val schema = universalGameSchema {
            meta {
                name = "Empty Collections Test"
                targetAge = intArrayOf(8, 12)
                participantCount = intArrayOf(2, 4)
            }
            
            objectTypes {
                objectType("player") { }
            }
            
            // These blocks should result in empty collections
            instances { }
            triggers { }
        }
        
        assertEquals(1, schema.objectTypes.size)
        assertEquals(0, schema.instances.size)
        assertEquals(0, schema.triggers.size)
    }
}