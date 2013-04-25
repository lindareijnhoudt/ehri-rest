# Setup of development environment

## Prerequisites

Install Oracle JDK 7. For Ubuntu this can be done via the Webupd8team PPA:

	sudo add-apt-repository ppa:webupd8team/java
	sudo apt-get update
	sudo apt-get install oracle-java7-installer 

Install Git:

	sudo apt-get install git

Install Curl:

	sudo apt-get install curl

Make somewhere to put downloaded Java apps:

	mkdir ~/apps

Download and install Maven:

	export MVN_VERSION=3.0.5
	curl -0 http://apache.mirrors.timporter.net/maven/maven-3/${MVN_VERSION}/binaries/apache-maven-${MVN_VERSION}-bin.tar.gz | tar -zx -C ~/apps

Add Maven to $PATH:

	export PATH=$PATH:$HOME/apps/apache-maven-${MVN_VERSION}/bin

(It's best to add these lines to your .bashrc or .bash_profile file too)


## Checking out the EHRI REST-server code

Make somewhere to put your code:

	mkdir ~/dev
	cd ~/dev

Clone the EHRI server code from Github:

	git clone https://github.com/mikesname/neo4j-ehri-plugin.git
	cd neo4j-ehri-plugin
	mvn clean install

(Maven will then proceed to download hundreds of megabytes of dependencies, but most of this will only have to be downloaded once.)

While Maven is doing its thing, we can install a standalone version of the Neo4j server. Once the code has been built, we can enable the EHRI REST service within the standalone Neo4j server.

	curl -L -0 "http://download.neo4j.org/artifact?edition=community&version=1.9.RC1&distribution=tarball&dlid=233206" | tar zx -C ~/apps
	
We'll need to refer to the installation location a lot so save it:

	export NEO4J_VERSION=1.9.RC1
	export NEO4J_HOME=$HOME/apps/neo4j-community-${NEO4J_VERSION}
	export NEO4J_DB=$NEO4J_HOME/data/graph.db

Once both the EHRI server code has been compiled and Neo4j standalone has been downloaded and extracted, we can install the former into the latter. First, however, we need to upgrade some libraries in the Neo4j dependencies. At present, for compatibility reasons, Neo4j uses an old version of a library called Blueprints. EHRI uses a more recent version. While this situation is unfortunate, it should be remedied before long. For the time being we have to replace some jars with newer versions. There is a script for this bundled with the EHRI code, which should be called giving Neo4j home as an argument:

	./scripts/replacejars.sh $NEO4J_HOME

Once that's done, we can run another script to install the EHRI libraries:

	./scripts/install.sh $NEO4J_HOME

Once that has completed the script will warn us that we have to make one configuation change manually:

IMPORTANT: You must manually ensure the /home/mike/apps/neo4j-community-1.9.RC1/conf/neo4j-server.properties configuration contains the line:
   org.neo4j.server.thirdparty_jaxrs_classes=eu.ehri.extension=/ehri

Open that file and add that line below the point where a similar example config is commented out. This step is vital so Neo4j knows to load our EHRI extension code.

At this point, if we go to http://localhost:7474 we should be able to see the Neo4j server admin console. However, the EHRI Rest server endpoints will error at the moment because we have not yet initialised the graph database properly. We can do this using one of the EHRI administration commands that can be run from the code root. First, make sure the Neo4j database server isn't running:

	$NEO4J_HOME/bin/neo4j stop

Then, referring to the location of the Neo4j _database_ direction rather than the installation home, run the following commands from the EHRI code root:

	./scripts/cmd $NEO4J_DB initialize

We also need to create an administrative user account:

	./scripts/cmd $NEO4J_DB useradd $USER --group admin

Now we can start the server again and check everything is working:

	$NEO4J_HOME/bin/neo4j start

	curl -H "Authorization:$USER" localhost:7474/ehri/userProfile/$USER

We should get the following output (with "mike" replaced by your $USER):

	```json
	{
	  "id" : "mike",
	  "data" : {
	    "name" : "mike",
	    "identifier" : "mike"
	  },
	  "type" : "userProfile",
	  "relationships" : {
	    "lifecycleEvent" : [ {
	      "id" : "211a5a7e-6430-4b51-b1fb-e8ba562a4118",
	      "data" : {
		"timestamp" : "2013-04-25T16:24:41.777+01:00",
		"logMessage" : "Created via command-line"
	      },
	      "type" : "systemEvent",
	      "relationships" : {
		"hasActioner" : [ {
		  "id" : "admin",
		  "data" : {
		    "name" : "Administrators",
		    "identifier" : "admin"
		  },
		  "type" : "group",
		  "relationships" : {
		  }
		} ]
	      }
	    } ],
	    "belongsTo" : [ {
	      "id" : "admin",
	      "data" : {
		"name" : "Administrators",
		"identifier" : "admin"
	      },
	      "type" : "group",
	      "relationships" : {
	      }
	    } ]
	  }
	}
	```