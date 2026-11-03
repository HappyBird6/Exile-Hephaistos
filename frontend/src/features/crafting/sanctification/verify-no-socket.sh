set -eu
mkdir -p /root/.gradle /tmp/project/backend /tmp/project/contracts /out/formatted
tar -C /cache --exclude='*.lock' --exclude=./daemon --exclude=./workers --exclude=./.tmp --exclude=./caches/journal-1 -cf - . | tar -C /root/.gradle -xf -
tar -C /source/backend --exclude=./.gradle --exclude=./build -cf - . | tar -C /tmp/project/backend -xf -
tar -C /source/contracts -cf - . | tar -C /tmp/project/contracts -xf -
cd /tmp/project/backend
bash ./gradlew --no-daemon spotlessApply > /out/java-format.log 2>&1
for file in src/main/java/com/poe2craft/crafting/domain/QualityCurrencyRules.java src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java src/test/java/com/poe2craft/crafting/QualitySanctificationTest.java src/test/java/com/poe2craft/crafting/QualityPlusOneTest.java; do cp --parents "$file" /out/formatted/; done
set +e
bash ./gradlew --no-daemon test --tests '*QualityPlusOneTest' --tests '*QualitySanctificationTest' --tests '*QualityLimitTest' --tests '*QualityCapChangeTest' --tests '*WorkbenchSimulatorTest' --tests '*ArchitectureTest' spotlessCheck bootJar --continue > /out/backend.log 2>&1
status=$?
if [ -d build/test-results ]; then mkdir -p /out/test-results; cp -r build/test-results/. /out/test-results/; fi
if [ -f build/libs/poe2craft.jar ]; then cp build/libs/poe2craft.jar /out/poe2craft.jar; fi
tail -50 /out/backend.log
exit $status
