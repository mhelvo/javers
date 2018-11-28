package org.javers.repository.mongo.cases

import com.mongodb.client.MongoDatabase
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.annotation.DiffIgnore
import org.javers.core.metamodel.annotation.Value
import org.javers.repository.mongo.MongoRepository
import spock.lang.Specification

import javax.persistence.Id
import javax.persistence.OneToMany

/**
 * Decouple cross reference object by DiffIgnore
 *
 * @author hank cp
 */
class CrossReferenceTest extends Specification {

    class CrossReferenceHost {
        @Id long id
        CrossReferenceObjectA a
    }

    @Value
    class CrossReferenceObjectA {
        @OneToMany
        List<CrossReferenceObjectB> bList

        @OneToMany
        @DiffIgnore
        List<CrossReferenceObjectB> bListIgnored
    }

    @Value
    class CrossReferenceObjectB {
        public int value;
        public CrossReferenceObjectA a

        CrossReferenceObjectB(int value, CrossReferenceObjectA a) {
            this.value = value
            this.a = a
        }
    }

    def "should not throw StackOverflowError exception for ValueType cross references"() {
        given:
        MongodForTestsFactory factory = MongodForTestsFactory.with(Version.Main.PRODUCTION)
        MongoDatabase mongo = factory.newMongo().getDatabase("test")

        def javers = JaversBuilder.javers()
                .registerJaversRepository(new MongoRepository(mongo))
                .build()

        when:
        def host = new CrossReferenceHost()
        host.id = 1
        host.a = new CrossReferenceObjectA()
        host.a.bListIgnored = [new CrossReferenceObjectB(1, host.a),
                               new CrossReferenceObjectB(2, host.a),
                               new CrossReferenceObjectB(3, host.a)]

        def commit = javers.commit("author", host)
        println commit

        then:
        commit
    }
}
