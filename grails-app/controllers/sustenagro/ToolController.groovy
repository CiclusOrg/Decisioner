package sustenagro

import com.github.slugify.Slugify
import com.github.rjeschke.txtmark.Processor
//import org.pegdown.PegDownProcessor
import rdfSlurper.DataReader
import rdfSlurper.RDFSlurper
import utils.Uri

import static rdfSlurper.RDFSlurper.N

class ToolController {
    def slp
    def dsl


    def index() {

        def inputs = []
        def query

        dsl.featureLst.each{
            def uri = '<'+slp.toURI(it[1])+'>'
            query = slp.query("$uri rdfs:label ?label. optional {$uri dc:description ?description}")
            if (query.empty) {
                query = slp.query("?id rdfs:label ?label. FILTER (STR(?label)='${it[1]}')", '')
                if (query.empty)
                    throw new RuntimeException('Unknown label: '+it[1])
                uri = "<${query[0].id}>"
                query = slp.query("$uri rdfs:label ?label. optional {$uri dc:description ?description}")
            }
            def lst = slp.query("?id ${it[0]} $uri ; rdfs:label ?label. optional {?id dc:description ?description}")
            inputs << [query[0].label, query[0].description, lst]
        }

        //println 'inp: '
        //inputs.each{
        //  println it
        //}
        //println Processor.process("This is ***TXTMARK***");
        //String html = new Markdown4jProcessor().process("This is a **bold** text");

        render(view: 'index',
               model:    [
                       name: dsl.name,
                       //description:   Processor.process(dsl.description),
                       description:   dsl.description,
                       microregions: inputs[0][2],
                       technologies: inputs[1][2],
                       production_unit_types: inputs[2][2],
                       production_units: inputs[3][2]])
    }

    def createProductionUnit() {
        Slugify slug = new Slugify()

        def production_unit_id = slug.slugify(params['production_unit_name'])

        slp.addNode(
            N(':'+production_unit_id,
                'rdf:type': slp.v(params['production_unit_type']),
                'rdfs:label': params['production_unit_name'],
                'dbp:Microregion': slp.v(params['production_unit_microregion']),
                //'sa:culture': slp.v(params['production_unit_culture']),
                ':AgriculturalEfficiency': slp.v(params['production_unit_technology'])
            ))

        slp.g.commit()
        //slp.g.saveRDF(new FileOutputStream('ontology/SustenAgroOntologyAndIndividuals.rdf'), 'rdf-xml')
        redirect(action: 'assessment', id: production_unit_id)
    }

    def selectProductionUnit(){
        def production_unit_alias = Uri.removeDomain(params['production_unit_id'], 'http://bio.icmc.usp.br/sustenagro#')

        redirect(   action: 'assessment',
                    id: production_unit_alias)
    }

    def selectEvaluation(){
        def production_unit_alias = Uri.removeDomain(params['production_unit_id'], 'http://bio.icmc.usp.br/sustenagro#')
        def evaluation_alias = Uri.removeDomain(params['evaluation'], 'http://bio.icmc.usp.br/sustenagro#')

        def indicators = slp.query("?id :compose :$evaluation_alias. ?id :value ?value")

        //println "indicators"
        //println indicators

        redirect(   action: 'assessment',
                id: production_unit_alias,
                model:    [indicators: indicators])
    }

    def evaluations(){
        def production_unit_id = Uri.removeDomain(params['production_unit_id'], 'http://bio.icmc.usp.br/sustenagro#')
        def evaluations = slp.query("?id :appliedTo :$production_unit_id. ?id rdfs:label ?label")

        render( template: 'evaluations',
                model:    [evaluations: evaluations]);
    }

    def assessment() {

        def indicators = {
            slp.select('?id ?title ?class ?valueType')
               .query( '?a  rdfs:subClassOf '+it+''' .
                        ?id rdfs:subClassOf ?a; rdfs:label ?title.
                        ?id rdfs:subClassOf ?y.
                        ?y  owl:onClass ?class.
                        ?class rdfs:subClassOf ?valueType.''')
        }

        def categorical = [:]

        def categ = {
            it.each{
                slp.query("<$it.id> rdfs:subClassOf ?a. ?a owl:onClass ?id").each{
                    categorical[it.id] = []
                }
            }
        }

        def environmental_indicators = indicators(':EnvironmentalIndicator')
        def economic_indicators = indicators(':EconomicIndicator')
        def social_indicators = indicators(':SocialIndicator')

        categ(environmental_indicators)
        categ(economic_indicators)
        categ(social_indicators)

        categorical.each{ k, v ->
            slp.query("?id a <$k>; rdfs:label ?title.").each{
                //it.id = slp.fromURI(it.id)
                v.push(it)
            }
        }

        //println categorical

        String name = slp.":$params.id".'$rdfs:label'
        println 'Recom: '+params['report']

        def report = params['report']
        if (report==null) report =[]

        render(view: 'assessment',
               model: [sustenagro: 'http://bio.icmc.usp.br/sustenagro#',
                       production_unit_id: params.id,
                       production_unit_name: name,
                       environmental_indicators: environmental_indicators,
                       economic_indicators: economic_indicators,
                       social_indicators: social_indicators,
                       categorical: categorical,
                       report: report])
    }

    def assessmentReport() {
        def production_unit_id = params['production_unit_id']

        def num = slp.query('?id a <http://bio.icmc.usp.br/sustenagro#Evaluation>. ?id <http://bio.icmc.usp.br/sustenagro#appliedTo> <http://bio.icmc.usp.br/sustenagro#' + production_unit_id +'>' ).size() + 1
        def evaluation_name = production_unit_id+"-evaluation-"+num

        slp.addNode(
            N(':'+evaluation_name,
                    'rdf:type': slp.v('http://bio.icmc.usp.br/sustenagro#Evaluation'),
                    ':appliedTo': slp.v(':'+production_unit_id) ,
                    'rdfs:label': 'Avaliação '+ num)
        )

        slp.g.commit()

        def indicators = []
        def inputs = [:]

        dsl.dimensions.each{
            indicators += slp.query("?a rdfs:subClassOf $it. ?id rdfs:subClassOf ?a.")
        }

        indicators.each{
            if(params[it.id]){
                //println "indicator: $it.id, value: " + slp.toURI2(params[name])
                inputs[it.id] = params[it.id]

                slp.addNode(
                        N(it.id+'-'+evaluation_name,
                                'rdf:type': slp.v(it.id),
                                'dc:isPartOf': slp.v(':'+evaluation_name),
                                ':value': slp.v(params[it.id]))
                )
                slp.g.addEdge(slp.v(':'+evaluation_name), slp.v(it.id+'-'+evaluation_name), 'http://purl.org/dc/terms/hasPart')
            }
        }
        
        slp.g.commit()

        println inputs

        def data = new DataReader(slp, slp.toURI(':'+evaluation_name))

        println 'Data: '+data.'OperationalEfficiencyPlant'
        println 'Data: '+data.'EnergyEfficiencyOfBoilersForCogeneration'
        println 'Data: '+data.'Eficiência operacional da Usina (crescimento vertical da usina, recuperação e avanço)'
        println 'Data: '+data.'Eficiência energética das caldeiras para cogeração de energia'

        dsl.data = data
        dsl.program()
        println 'indice: '+ dsl.environment
        println 'indice: '+ dsl.economic
        println 'indice: '+ dsl.social

        //def recommendations = []
        //dsl.recommendations.each{if (it[0]()) recommendations << new PegDownProcessor().markdownToHtml(it[1])}

        //println 'Recom1: '
        //recommendations.each{println it}

        redirect(action: 'assessment',
                id: params['production_unit_id'],
                params: [report: dsl.report])
    }
}