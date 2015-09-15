import rdfSlurper.RDFSlurper
import dsl.DSL

// Place your Spring DSL code here
beans = {
    slp(RDFSlurper, 'http://java.icmc.usp.br:9999/bigdata/sparql')
    dsl(DSL, 'dsl.groovy')
}
