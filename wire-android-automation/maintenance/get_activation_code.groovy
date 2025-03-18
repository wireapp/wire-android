properties([
    parameters([
        choice(choices: ['https://staging-nginz-https.zinfra.io', 'https://edge-nginz-https.zinfra.io'], description: 'Select either staging or edge backend', name: 'BackendUrl'),
        string(defaultValue: '+0', description: '8 to 16 characters', name: 'PhoneNumber', trim: false)
    ])
])

node('WebApp_Linux') {

    env.BackendUrl = params.BackendUrl
    env.PhoneNumber = params.PhoneNumber

    withCredentials([string(credentialsId: "EDGE_BACKEND_BACKDOOR_PASSWORD", variable: 'EDGE_BACKEND_BACKDOOR_PASSWORD'),string(credentialsId: "STAGING_BACKEND_BACKDOOR_PASSWORD", variable: 'STAGING_BACKEND_BACKDOOR_PASSWORD')]) {
        sh '''
TMP_PATH=$WORKSPACE/tmp.txt
ARTIFACT_PATH=$WORKSPACE/user_info.txt
EDGE_BACKEND_BACKDOOR_USER=wire-edge
STAGING_BACKEND_BACKDOOR_USER=wire-staging

# Cleanup
rm -f $WORKSPACE/*.txt

if test "${BackendUrl#*edge}" != "$BackendUrl"; then
    credentials=$EDGE_BACKEND_BACKDOOR_USER:$EDGE_BACKEND_BACKDOOR_PASSWORD
elif test "${BackendUrl#*staging}" != "$BackendUrl"; then
    credentials=$STAGING_BACKEND_BACKDOOR_USER:$STAGING_BACKEND_BACKDOOR_PASSWORD
else
    echo "Unknown backend url $BackendUrl"
    exit 1
fi

EncodedPhoneNumber=`echo $PhoneNumber | cut -d+ -f 2`

curl -s \
   --user $credentials \
   "$BackendUrl/i/users/login-code?phone=%2B$EncodedPhoneNumber" > $TMP_PATH

cat $TMP_PATH

echo "Backend URL     : $BackendUrl" > $ARTIFACT_PATH
echo "Timestamp       : $(date)" >> $ARTIFACT_PATH
echo "Phone Number    : $PhoneNumber" >> $ARTIFACT_PATH
activation_code=`cat $TMP_PATH | jq -r .code`
echo "Activation Code      : $activation_code" >> $ARTIFACT_PATH

rm -f $TMP_PATH

'''
    }

    archiveArtifacts '*.txt'
}