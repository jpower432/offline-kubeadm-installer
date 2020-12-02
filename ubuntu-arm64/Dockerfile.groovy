# Build the binary
FROM ubuntu:groovy AS build-stage
RUN mkdir -p /build/payload/images
RUN mkdir /build/payload/packages
RUN mkdir /build/payload/manifests
RUN mkdir /build/payload/dependencies
WORKDIR /build

# Copy in images
COPY images/* /build/payload/images/

# Copy in binary scripts
COPY installer payload/
COPY decompress .
COPY build .

# Setup Repos and prepare dependency downloads
RUN apt-get update && apt-get install -y apt-transport-https ca-certificates curl software-properties-common gnupg2
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -
RUN curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add -
RUN echo "deb https://apt.kubernetes.io/ kubernetes-xenial main" > /etc/apt/sources.list.d/kubernetes.list
RUN add-apt-repository "deb [arch=arm64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
RUN apt-get update -y

# Pull yum packages
RUN apt-get install -y --download-only containerd.io kubeadm kubectl kubelet
RUN cp /var/cache/apt/archives/*.deb payload/packages/

# Pull the flannel manifest
RUN curl -L https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml -o payload/manifests/kube-flannel.yaml

# Pull crictl
RUN curl -L https://github.com/kubernetes-sigs/cri-tools/releases/download/v1.19.0/critest-v1.19.0-linux-arm64.tar.gz --output payload/dependencies/crictl-1.19.0-linux-arm64.tar.gz

# Build it
RUN bash build

# Export the binary
FROM scratch AS export-stage
COPY --from=build-stage /build/selfextract.bsx /