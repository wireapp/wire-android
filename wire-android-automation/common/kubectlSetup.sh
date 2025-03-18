#!/bin/bash

# Install and set Up kubectl on MacOS and Linux.
# For more details, see https://kubernetes.io/docs/tasks/tools/install-kubectl-macos/
# or https://kubernetes.io/docs/tasks/tools/install-kubectl-linux/

PLATFORM=`uname | tr '[:upper:]' '[:lower:]'`

if [ -z ${WORKSPACE+x} ]; then
    # Running locally
    KUBECTLPATH="/usr/local/bin/"
else
    # Script runs on Jenkins
    KUBECTLPATH="${WORKSPACE}"
fi

VERSION="$(curl -L -o - https://dl.k8s.io/release/stable.txt)"
echo $VERSION

if [ ! -e ${KUBECTLPATH}/kubectl ] || [[ $(${KUBECTLPATH}/kubectl version --client) != **$VERSION** ]]; then

	# Delete former version if exists
	rm -rf kubectl kubectl.sha256

	# Download the latest release
	curl -LO "https://dl.k8s.io/release/${VERSION}/bin/${PLATFORM}/amd64/kubectl"

	# Download the kubectl checksum file
	curl -LO "https://dl.k8s.io/release/${VERSION}/bin/${PLATFORM}/amd64/kubectl.sha256"

	# Validate the kubectl binary against the checksum file
	if echo "$(<kubectl.sha256)  kubectl" | shasum -a 256 --check; then 

            # Make the kubectl binary executable
            chmod +x ./kubectl

            if [ -z ${WORKSPACE+x} ]; then
                # Move the kubectl binary to a file location on your system PATH
                sudo mv ./kubectl /usr/local/bin/kubectl
                sudo chown root: /usr/local/bin/kubectl
            else
                mv ./kubectl $WORKSPACE/kubectl
            fi

            # Print out version
            ${KUBECTLPATH}/kubectl version --client

            echo 'Success!'
  else
    echo 'Wrong or invalid checksum after download of kubectl'
  fi

else
  echo 'kubectl already installed'
fi
