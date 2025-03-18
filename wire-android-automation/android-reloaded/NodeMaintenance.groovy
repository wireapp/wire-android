node('built-in') {

    env.PATH = "$PATH:/usr/local/bin"
    env.NODE_LABELS = "android_tablet || android"

    def jenkinsbot_secret = ""
    withCredentials([string(credentialsId: "JENKINSBOT_NEW_ANDROID_AUTOMATION", variable: 'JENKINSBOT_SECRET')]) {
        jenkinsbot_secret = env.JENKINSBOT_SECRET
    }

    nodes = nodesByLabel label: "$NODE_LABELS", offline: true

    stage('Check availability of nodes') {
        nodes.each {
            echo("Checking if node " + it + " is online...")
            if (Jenkins.instance.getNode(it).toComputer().isOnline()) {
                echo("Checking if node " + it + " is busy...")
                for (slave in hudson.model.Hudson.instance.slaves) {
                    if (slave.name.equals(it)) {
                        final comp = slave.getComputer()
                        if (comp.isOnline()) {
                            if (comp.countBusy() > 0) {
                                currentBuild.result = 'ABORTED'
                                error(slave.getNodeName() + " seems to be busy. Aborting the build...")
                            }
                        }
                    }
                }
            } else {
                echo("Node " + it + " is offline!")
            }
        }
    }

    def offline_nodes = []

    stage('Delete Appium and Old App') {
        def jobs = [:]
        nodes.each {
                if (Jenkins.instance.getNode(it).toComputer().isOnline()) {
                    // If onGrid, go thru each usb-connected device
                    node(it) {
                        timeout(activity: true, time: 5) {
                            sh returnStatus: true, script: '''
for i in $(adb devices | sed 1d | cut -f 1 ); do
    adb -s $i shell pm list packages | grep -e wire -e appium -e zclient | grep -v testinggallery | cut -d: -f2 | xargs -I APP adb -s $i uninstall APP
    adb -s $i reboot && adb -s $i wait-for-device
done
'''
                        }
                        wireSend secret: "$jenkinsbot_secret", message: "Grid maintenance: " + it
                    }
                } else {
                    // The old script went to the parallels host machine and killed the parallels process
                    echo("Node " + it + " is offline!")
                    offline_nodes.add(it)
                }
        }
        parallel jobs
    }

    if (offline_nodes.size() > 0) {
        echo("Offline nodes: " + offline_nodes.join(","))
        wireSend secret: "$jenkinsbot_secret", message: "Several Android nodes are offline: " + offline_nodes.join(", ")
    }
}


