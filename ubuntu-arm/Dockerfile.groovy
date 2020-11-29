# Build the binary
FROM ubuntu:groovy AS build-stage
RUN mkdir -p /build/payload/images
RUN mkdir /build/payload/packages
RUN mkdir /build/payload/manifests
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
RUN add-apt-repository "deb [arch=armhf] https://download.docker.com/linux/ubuntu bionic stable"
RUN apt-get update -y

# Pull yum packages
RUN apt-get install -y --download-only containerd.io kubeadm kubectl kubelet
RUN cp /var/cache/apt/archives/*.deb payload/packages/

# Pull the weave-net manifest
RUN curl https://cloud.weave.works/k8s/v1.16/net.yaml -o payload/manifests/weave-net.yaml

# Build it
RUN bash build


# Export the binary
FROM scratch AS export-stage
COPY --from=build-stage /build/selfextract.bsx /