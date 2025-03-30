import groovy.json.JsonSlurper
import hudson.model.AbstractProject
import hudson.model.ParametersAction
import hudson.model.AbstractBuild
import hudson.model.Result
import hudson.util.RunList
import jenkins.model.Jenkins

final build = Thread.currentThread().executable
final envVarsMap = build.properties.get("envVars")

final jobName = "android_master_test"
final expectedBranch = "master"
final expectedCoverageNameSet = (envVarsMap['COVERAGES'].split(','))*.trim() as Set
final RATE_THRESHOLD = envVarsMap['THRESHOLD'] as Integer
final String JOB_BUILDS_HOME = "/Users/Shared/Jenkins/Home/jobs/android_master_test/builds/"
final String REPORT_PATH = "/cucumber-html-reports/report.json"
final String ENV_VARS_PATH = "/injectedEnvVars.txt"

//--------------------------------------------- FUNCTIONS---------------------------------------------------------------

def log(msg) {
    out.println(msg)
}

class TestCase {
    String id
    String name
    String result
}

static def isBuildDateToday(AbstractBuild bld) {
    final Calendar todayCalendar = GregorianCalendar.getInstance(TimeZone.getDefault())
    final today = "${todayCalendar.get(Calendar.YEAR)}${todayCalendar.get(Calendar.MONTH)}${todayCalendar.get(Calendar.DAY_OF_MONTH)}" as Object
    final timestamp = bld.getTimestamp() as GregorianCalendar
    final buildDate = "${timestamp.get(Calendar.YEAR)}${timestamp.get(Calendar.MONTH)}${timestamp.get(Calendar.DAY_OF_MONTH)}" as Object
    return buildDate == today
}

static def getBuilds(RunList buildList,String expectedBranchName, Set expectedCoverageSet) {
    buildList.completedOnly().findAll {
        isBuildDateToday(it)
    }.findAll {
        //Search all builds for branch
        def resolver = it.getActions(ParametersAction).get(0).createVariableResolver(it)
        def branch = resolver.resolve("Branch")
        branch == expectedBranchName
    }.findAll {
        def resolver = it.getActions(ParametersAction).get(0).createVariableResolver(it)
        def coverage = resolver.resolve("Coverage")
        expectedCoverageSet.contains(coverage)
    } as ArrayList
}

static def getElementId(Object cucumberReportElementTags) {
    return cucumberReportElementTags.find { it.name =~ /^@C.*/ }.name.replaceAll("@","")
}

//returns [totalTestCount, successTestCount, testCases], according to cucumber report file
static def getBuildResult(String buildHome, Integer buildNumber, String reportPath) {
    def report = new File("$buildHome$buildNumber$reportPath").readLines()
    def featureList = new JsonSlurper().parseText(report)
    def testsTotal = 0
    def testsPassed = 0
    def testCases = []

    featureList.each {
        testsTotal += it.elements.size()
        // iterate test cases
        it.elements.each {
            def stepsTotal = it.steps.size()
            def stepsPassed = 0
            // iterate through steps
            it.steps.each {
                if (it.result.status == "passed") stepsPassed++
            }
            if (stepsTotal == stepsPassed) testsPassed++

            testCases << new TestCase(id: getElementId(it.tags), name: it.name, result: stepsTotal == stepsPassed)
        }
    }
    [testsTotal, testsPassed, testCases]
}

static def sumResults(ArrayList resultList) {
    def testsTotal = 0
    def testsPassed = 0
    resultList.each { count, passed, _ ->
        testsTotal += count
        testsPassed += passed
    }
    [testsTotal, testsPassed]
}

static def produceJunitReport(Integer total, Integer passed, ArrayList<TestCase> testCases, File report) {
    report << """<testsuite disabled="" errors="${total - passed}" failures="" hostname="" id="" name="" package="" skipped="" tests="${total}" time="" timestamp="">"""
    for (int i = 0; i < total; i++) {
        def testCase = testCases.get(i)
        def id = testCase.id
        def name = testCase.name.replaceAll("\"","'")
        if (new Boolean(testCase.result)) {
            report << """<testcase assertions="" classname="$id" name="$name" status="PASSED" time=""></testcase>"""
        } else {
            report << """<testcase assertions="" classname="$id" name="$name" status="FAILED" time=""><failure/></testcase>"""
        }
    }
    report << """</testsuite>"""
}

static def getInternalBuildNumber(String buildHome, Integer buildNumber, String reportPath) {
    def envFileLines = new File("$buildHome$buildNumber$reportPath").readLines()
    for (String line : envFileLines) {
        def trimLine = line.trim()
        if (trimLine.startsWith("REAL_BUILD_NUMBER=")) {
            return trimLine.split("=")[1]
        }
    }
    return ""
}

//----------------------------------------------------------------------------------------------------------------------

final AbstractProject masterJob = Jenkins.getInstance().getItemByFullName(jobName, AbstractProject.class)
final RunList builds = masterJob.getBuilds()
ArrayList foundBuilds = getBuilds(builds, expectedBranch, expectedCoverageNameSet)

log(" --> Found builds: $foundBuilds")
log(" --> Expected coverages: $expectedCoverageNameSet")
final readyBuildNamesSet = foundBuilds.collect { it.getActions(ParametersAction).get(0).createVariableResolver(it).resolve("Coverage") } as Set
if (readyBuildNamesSet != expectedCoverageNameSet) {
    final missingCoveragesSet = expectedCoverageNameSet - readyBuildNamesSet
    log(" --> Not all coverages have been executed yet. Missing coverages: ${missingCoveragesSet}")
    log("Exiting...")
    throw new InterruptedException()
}

ArrayList resultList = foundBuilds.collect {
    getBuildResult(JOB_BUILDS_HOME, it.getNumber() as Integer, REPORT_PATH)
} as ArrayList

def (Integer total, Integer passed) = sumResults(resultList)
ArrayList<TestCase> testCases = []
resultList.each { t, p, ArrayList<TestCase> testCaseList ->
        testCases << testCaseList
}
testCases = testCases.flatten()


log("The current run finished with result: $total total cases, where $passed are passed" as String)
final rate = Math.round((passed / total) * 10000) / 100
final internal_build = getInternalBuildNumber(JOB_BUILDS_HOME, foundBuilds[0].getNumber() as Integer,ENV_VARS_PATH)
final File rateFile = new File("${envVarsMap.get('WORKSPACE')}/rate.properties")
rateFile.delete()
rateFile << "RATE=${rate}\n"
rateFile << "INTERNAL_BUILD=${internal_build}"


final File reportFile = new File("${envVarsMap.get('WORKSPACE')}/report.xml")
reportFile.delete()
produceJunitReport(total, passed, testCases, reportFile)