#!/usr/bin/env python3
"""Fixture syntax only using cached Kotlin parser; no typecheck, Metro plugin, Gradle or project build."""
import os, pathlib, subprocess
from repo_paths import SOURCE as HERE, OUTPUT
cache=pathlib.Path.home()/'.gradle/caches/modules-2/files-2.1'
java=pathlib.Path(os.environ.get('JAVA_HOME') or subprocess.check_output(['/usr/libexec/java_home','-v','21'],text=True).strip())
def one(group,artifact,version):
    hits=[x for x in (cache/group/artifact/version).glob('*/*.jar') if x.name == artifact+'-'+version+'.jar']
    if len(hits)!=1: raise SystemExit('Expected one cached JAR: '+artifact+' '+version)
    return str(hits[0])
for version,variant in [('2.3.20','baseline'),('2.4.20','current')]:
    cp=os.pathsep.join([one('org.jetbrains.kotlin','kotlin-compiler-embeddable',version),one('org.jetbrains.kotlin','kotlin-stdlib',version),one('org.jetbrains.kotlinx','kotlinx-coroutines-core-jvm','1.10.2')])
    classes=OUTPUT/'smoke'/('parser-'+version); classes.mkdir(parents=True, exist_ok=True)
    subprocess.run([str(java/'bin/javac'),'-cp',cp,'-d',str(classes),str(HERE/'smoke/ParseKotlin.java')],check=True)
    subprocess.run([str(java/'bin/java'),'-cp',cp+os.pathsep+str(classes),'ParseKotlin',str(HERE/'fixtures'/(variant+'.kt'))],check=True)

# Parse only; does not launch Gradle or configure any project.
groovy = list((pathlib.Path.home()/'.gradle/wrapper/dists/gradle-9.4.1-all').glob('*/gradle-9.4.1/lib/groovy-4.0.29.jar'))
if len(groovy) != 1: raise SystemExit('Expected cached Gradle Groovy parser JAR')
classes=OUTPUT/'smoke/groovy-parser'; classes.mkdir(parents=True, exist_ok=True)
subprocess.run([str(java/'bin/javac'),'-cp',str(groovy[0]),'-d',str(classes),str(HERE/'smoke/ParseGroovy.java')],check=True)
subprocess.run([str(java/'bin/java'),'-cp',str(groovy[0])+os.pathsep+str(classes),'ParseGroovy',str(HERE/'measurement.init.gradle')],check=True)
