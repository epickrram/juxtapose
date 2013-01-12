# Copyright 2011 Mark Price
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#!/bin/sh

rm -rf ./build
mkdir ./build


CP="."; for i in `find ./lib/ -iname '*.jar'`; do CP="$CP:$i"; done

find ./src/main/java -iname '*.java' > /tmp/filelist.txt

javac -classpath $CP @/tmp/filelist.txt -d ./build

mkdir -p ./dist

cd ./build

jar cf ../dist/juxtapose-1.0.jar *

rm -rf ./build
