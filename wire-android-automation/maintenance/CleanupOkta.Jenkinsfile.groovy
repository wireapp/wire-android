/*properties([
        parameters([
                string(name: 'FIXSSO_APPID', defaultValue: '', description: 'Exclude certain okta app id from being deleted (Fixed SSO account)', trim: false),
                credentials(name: 'JENKINSBOT_SECRET', credentialType: 'org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl', defaultValue: 'JENKINSBOT_QA_MAINTENANCE', required: true)
        ])
])
*/

node('Job_distributor') {
    env.PATH = "$PATH:/usr/local/bin"

    stage('Prepare software') {
        sh """
if [ ! -f ./jq ]; then
  curl https://github.com/stedolan/jq/releases/download/jq-1.6/jq-osx-amd64 -L -o jq
  chmod +x jq
fi
"""
    }

    stage('Cleanup okta') {

        withCredentials([string(credentialsId: "OKTA_API_KEY", variable: 'API_KEY'), string(credentialsId: params.JENKINSBOT_SECRET, variable: 'JENKINSBOT_SECRET')]) {

            try {
                def BASEURL = "https://dev-500508-admin.oktapreview.com"

                env.FIXSSO_APPID = params.FIXSSO_APPID

                sh """
APPIDS=`curl -X GET -H "Content-Type: application/json" -H "Authorization: SSWS ${API_KEY}" "${BASEURL}/api/v1/apps?limit=100" | ./jq '.[] | select(.label != "Wrongly configured application") | .id' | sed 's/"//g'`

for APPID in \$APPIDS; do
  if [ \${APPID} = \${FIXSSO_APPID} ] ; then
    echo "Exclude deletion of FIXSSO AppId: \${FIXSSO_APPID}"
  else
    curl -X POST -H "Content-Type: application/json" -H "Authorization: SSWS ${API_KEY}" ${BASEURL}/api/v1/apps/\${APPID}/lifecycle/deactivate
    curl -X DELETE -H "Content-Type: application/json" -H "Authorization: SSWS ${API_KEY}" ${BASEURL}/api/v1/apps/\${APPID}
  fi
done
"""


                sh """
while [ `curl -X GET -H "Content-Type: application/json" -H "Authorization: SSWS ${API_KEY}" ${BASEURL}/api/v1/users?limit=2 2>/dev/null | ./jq '.[] | .id' | wc -l` -gt 1 ]
do
    curl -X GET -H "Content-Type: application/json" -H "Authorization: SSWS ${API_KEY}" ${BASEURL}/api/v1/users?limit=100 2>/dev/null | ./jq '.[] | .id' | sed 's/"//g' | while read user
    do
        curl -X DELETE -H "Content-Type: application/json" -H "Authorization: SSWS ${API_KEY}" ${BASEURL}/api/v1/users/\$user?sendEmail=false
        curl -X DELETE -H "Content-Type: application/json" -H "Authorization: SSWS ${API_KEY}" ${BASEURL}/api/v1/users/\$user?sendEmail=false
    done
done
"""
            } catch (e) {
                wireSend secret: env.JENKINSBOT_SECRET, message: "**Cleanup okta failed**\nSee: ${JOB_URL}"
                throw e
            }
            wireSend secret: env.JENKINSBOT_SECRET, message: "**Cleanup okta finished**"
        }
    }

}
