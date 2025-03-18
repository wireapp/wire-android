properties([
        parameters([
                choice(choices: ['https://staging-nginz-https.zinfra.io', 'https://edge-nginz-https.zinfra.io'], description: 'Select either staging or edge backend', name: 'BackendUrl'),
                [$class: 'DynamicReferenceParameter',
                 choiceType: 'ET_FORMATTED_HTML',
                 description: 'Users phone number (8 to 15 digits)',
                 omitValueField: true,
                 name: 'PhoneNumber',
                 referencedParameters: 'PhoneNumber',
                 script: [$class: 'GroovyScript',
                          fallbackScript: [
                                  classpath: [],
                                  sandbox: true,
                                  script: 'return "error"'
                          ],
                          script: [
                                  classpath: [],
                                  sandbox: false,
                                  script: """
			long numbers = (long) Math.floor(Math.random() * 9000000000L) + 1000000000L;
			return "<input name='value' value='+0" + numbers + "' class='setting-input' type='text'>"
		    """
                          ]
                 ]
                ],
                booleanParam(defaultValue: false, description: '''Tick this option if you want to register the newly created user with phone number immediately. In case you want to book the phone number only and continue with manual registration then keep it as is.''', name: 'ShouldRegisterUser'),
                [$class: 'DynamicReferenceParameter',
                 choiceType: 'ET_FORMATTED_HTML',
                 description: 'Name of user',
                 omitValueField: true,
                 name: 'Name',
                 referencedParameters: 'Name',
                 script: [$class: 'GroovyScript',
                          fallbackScript: [
                                  classpath: [],
                                  sandbox: true,
                                  script: 'return "error"'
                          ],
                          script: [
                                  classpath: [],
                                  sandbox: true,
                                  script: """
                        return "<input name='value' value='" + (java.util.UUID.randomUUID() as String)[0..7] + "' class='setting-input' type='text'>"
                    """
                          ]
                 ]
                ]
        ])
])

node('WebApp_Linux') {

    env.BackendUrl = params.BackendUrl
    env.PhoneNumber = params.PhoneNumber
    env.ShouldRegisterUser = params.ShouldRegisterUser
    env.Name = params.Name

    withCredentials([string(credentialsId: "EDGE_BACKEND_BACKDOOR_PASSWORD", variable: 'EDGE_BACKEND_BACKDOOR_PASSWORD'),string(credentialsId: "STAGING_BACKEND_BACKDOOR_PASSWORD", variable: 'STAGING_BACKEND_BACKDOOR_PASSWORD')]) {
        sh '''
TMP_PATH=$WORKSPACE/tmp.txt
ARTIFACT_PATH=$WORKSPACE/user_info.txt
EDGE_BACKEND_BACKDOOR_USER=wire-edge
STAGING_BACKEND_BACKDOOR_USER=wire-staging

# Cleanup
rm -f $WORKSPACE/*.txt

curl -s \
  -XPOST $BackendUrl/activate/send \
  -H "Content-type: application/json" \
  -d "{ \\"phone\\":\\"$PhoneNumber\\" }" > $TMP_PATH

cat $TMP_PATH

if [ -n "`cat $TMP_PATH`" ]; then
  exit 1
fi

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
   "$BackendUrl/i/users/activation-code?phone=%2B$EncodedPhoneNumber" > $TMP_PATH

cat $TMP_PATH

echo "Backend URL     : $BackendUrl" > $ARTIFACT_PATH
echo "Phone Number    : $PhoneNumber" >> $ARTIFACT_PATH
# activation_key=`cat $TMP_PATH | jq -r .key`
# echo "Activation Key  : $activation_key" >> $ARTIFACT_PATH
activation_code=`cat $TMP_PATH | jq -r .code`
echo "Activation Code : $activation_code" >> $ARTIFACT_PATH

if test "$ShouldRegisterUser" != "true"; then
    exit
fi

curl -s \
  -XPOST $BackendUrl/activate \
  -H"Content-type: application/json" \
  -d"{ \\"code\\":\\"$activation_code\\", \\"phone\\":\\"$PhoneNumber\\", \\"dryrun\\":true }" > $TMP_PATH

cat $TMP_PATH

if [ -n "`cat $TMP_PATH`" ]; then
   exit 1
fi

curl -s \
  -XPOST $BackendUrl/register \
  -H"Content-type: application/json" \
  -d"{ \\"name\\":\\"$Name\\", \\"phone\\":\\"$PhoneNumber\\", \\"phone_code\\": \\"$activation_code\\" }" > $TMP_PATH

cat $TMP_PATH

#python -c \
#  "import json,sys; sys.exit(0) if json.load(open('$TMP_PATH', 'r'))['phone'] == '$PhoneNumber' else sys.exit(1)"

echo "User Name       : $Name" >> $ARTIFACT_PATH

rm -f $TMP_PATH

'''
    }

    archiveArtifacts '*.txt'
}