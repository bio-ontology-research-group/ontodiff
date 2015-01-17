import groovy.json.*
import java.util.logging.Logger
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.reasoner.*
import org.semanticweb.owlapi.profiles.*
import org.semanticweb.owlapi.util.*
import org.semanticweb.owlapi.io.*
import org.semanticweb.elk.owlapi.*

OWLOntologyManager manager1 = OWLManager.createOWLOntologyManager()
OWLOntologyManager manager2 = OWLManager.createOWLOntologyManager()

OWLDataFactory fac = manager1.getOWLDataFactory()

OWLOntology ont1 = manager1.loadOntologyFromOntologyDocument(new File(args[0]))
OWLOntology ont2 = manager2.loadOntologyFromOntologyDocument(new File(args[1]))
OWLReasonerFactory reasonerFactory = new ElkReasonerFactory()
OWLReasoner reasoner1 = reasonerFactory.createReasoner(ont1)
OWLReasoner reasoner2 = reasonerFactory.createReasoner(ont2)

OWLClass root = null
if (args.size() > 2 && args[2] != null) {
  root = fac.getOWLClass(IRI.create(args[2]))
} else {
  root = fac.getOWLThing()
}

reasoner1.precomputeInferences(InferenceType.CLASS_HIERARCHY)
reasoner2.precomputeInferences(InferenceType.CLASS_HIERARCHY)

List sameClasses = []
List newClasses = []
List oldClasses = []
Map sameEdges = [:].withDefault { new LinkedHashSet() }
Map newEdges = [:].withDefault { new LinkedHashSet() }
Map oldEdges = [:].withDefault { new LinkedHashSet() }
ont2.getClassesInSignature(true).each { cl1 ->
  if (ont1.containsClassInSignature(cl1.getIRI(), true)) {
    sameClasses << cl1
  } else {
    newClasses << cl1
  }
  reasoner2.getSubClasses(cl1, true).getFlattened().each { sup2 ->
    if (sup2 in newClasses || cl1 in newClasses) {
      newEdges[cl1].add(sup2)
    } else {
      if (sup2 in reasoner1.getSubClasses(cl1, true).getFlattened()) {
	sameEdges[cl1].add(sup2)
      } else {
	newEdges[cl1].add(sup2)
      }
    }
  }
}
ont1.getClassesInSignature(true).each { cl1 ->
  if (! ont2.containsClassInSignature(cl1.getIRI(), true)) {
    oldClasses << cl1
  }
  reasoner1.getSubClasses(cl1, true).getFlattened().each { sup1 ->
    if (sup1 in oldClasses || cl1 in oldClasses) {
      oldEdges[cl1].add(sup1)
    } else {
      if (!(sup1 in sameEdges[cl1])) {
	oldEdges[cl1].add(sup1)
      }
    }
  }
}

List l = []
reasoner2.getSubClasses(root, true).getFlattened().each { sub ->
  l << sub.toString()
}
l = l.collect { ["name":it.toString(), "id":it.toString()] }

def jb = new JsonBuilder(l)
println jb.toPrettyString()
sameClasses = sameClasses.collect { it.toString() }

println "New classes: $newClasses"
println "Same classes: "+new JsonBuilder(sameClasses).toString()
println "Old classes: $oldClasses"
println "New edges: $newEdges"
println "Same edges: $sameEdges"
println "Old edges: $oldEdges"
