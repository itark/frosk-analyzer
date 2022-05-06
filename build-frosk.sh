echo "Bygger frosk-analyzer."

echo "Publicerar gdax-java api till local maven..."
cd /Users/fredrikmoller/itark/git/gdax-java
./gradlew api:publishToMavenLocal
echo "gdax api published to local maven!"

echo "Publicerar gdax-java model till local maven..."
cd /Users/fredrikmoller/itark/git/gdax-java
./gradlew model:publishToMavenLocal
echo "gdax model published to local maven!"


echo "Publicerar gdax-java security till local maven..."
cd /Users/fredrikmoller/itark/git/gdax-java
./gradlew security:publishToMavenLocal
echo "gdax security published to local maven!"

echo "Publicerar gdax-java websocketfeed till local maven..."
cd /Users/fredrikmoller/itark/git/gdax-java
./gradlew websocketfeed:publishToMavenLocal
echo "gdax websocketfeed published to local maven!"

echo "Bygger frosk-analyxer..."
cd /Users/fredrikmoller/itark/git/frosk-analyzer/
mvn clean install -DskipTests
echo "frosk-analyxer byggd!"

echo "Startar frosk-analyxer..."
cd /Users/fredrikmoller/itark/git/frosk-analyzer/
mvn spring-boot:run
echo "frosk-analyser startad!"
