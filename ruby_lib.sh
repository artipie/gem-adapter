set -e
set -x
if [ ! -d "./lib" ]
then
  gem fetch builder
  BUILDER_GEM_FILE=$(ls | grep builder*\.gem | awk '{print $NF}')
  GEM_PATH=lib GEM_HOME=lib ruby -S gem install ./$BUILDER_GEM_FILE --no-document
  rm $BUILDER_GEM_FILE
fi
