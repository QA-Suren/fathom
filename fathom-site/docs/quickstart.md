## Quickstart

The fastest way to get started building an application or microservice based on Fathom is to use a [Maven archetype].  An archetype is a project templating toolkit which is used to generate a new project based on a few input parameters.

You'll need to install [Maven] for your operating system and have it available on your $PATH in order to use the following instructions.  You may also create your app using the archetype from an IDE.

### Creating Your App with Maven & the Fathom Standard Archetype

To create your app copy & paste the following command to your console:

```
mvn -B archetype:generate \
  -DarchetypeGroupId=com.gitblit.fathom \
  -DarchetypeArtifactId=fathom-archetype-standard \
  -DarchetypeVersion=0.5.2 \
  -DgroupId=com.mycompany \
  -DartifactId=myapp \
  -Dversion=1.0.0-SNAPSHOT \
  -DpackageName=com.mycompany \
  -Dname="My App"
```

Make sure to substitute the *groupId*, *artifactId*, *version*, *packageName*, and *name* values with ones that make sense for your app.

!!! Note
    After this command completes your new project will be available in the `artifactId` subdirectory of your current directory.

!!! Warning
    If you are creating your app with an IDE then you will have to configure your IDE compiler settings to use the `-parameters` compiler flag. See [this page](maven.md) for more details.

### About your Generated App

The project you created from the Fathom Standard Archetype is not a barebones project.  The archetype starts you off with with a completely functional webapp based on:

- [Fathom-Core](about.md)
- [Fathom-Eventbus](eventbus.md) for decoupled event passing
- [Fathom-REST](rest.md) + [pippo-freemarker] + [pippo-gson] for XML/JSON RESTful routing and HTML page generation
- [Fathom-Security](security.md) + [Fathom-REST-Security](rest-security.md) for a complete authentication & authorization infrastructure
- [Fathom-JCache](jcache.md) + [Infinispan] for seamless caching
- [Fathom-Mailer](mail.md) for email sending
- [Fathom-Quartz](quartz.md) for scheduled tasks
- [Fathom-Metrics](metrics.md) for runtime metric collection & reporting
- [Fathom-Test-Tools](testing.md) for unit & integration tests

The Fathom Standard Archetype also sets up your application for optimal [building with Maven and Java 8](maven.md).

### Using your Application

Your generated app is ready to perform.

#### Unit Tests

From your project directory...

```
mvn test
```

This will compile your application and run through the generated unit tests.  Some basic unit tests are included in the Fathom Standard Archetype that confirm your RESTful routing is working and demonstrate how to run your Fathom application in TEST mode.

#### Execution

You may also want to run your application and browse it.  Generally you would do this from your IDE by running the generated `App` class in your specified *application package*.  However there may be some value to compiling and running it from a console through Maven.

From your project directory...

```
mvn compile exec:java
```

... and then browse to [http://localhost:8080](http://localhost:8080).

### Packaging your Application

Your application has been preconfigured to generate both a Stork `.tar.gz` distribution and Capsule *thin-jar* & *fat-jar* binaries.

From your project directory...

```
mvn package
```

... and then find the generated binaries in the `target/` subdirectory.

[Maven]: http://maven.apache.org/
[Maven archetype]: https://maven.apache.org/guides/introduction/introduction-to-archetypes.html
[pippo-freemarker]: http://www.pippo.ro/doc/templates.html
[pippo-gson]: http://www.pippo.ro/doc/content-types.html
[Infinispan]: http://infinispan.org
[ZURB Foundation]: http://foundation.zurb.com/
[Font-Awesome]: https://fortawesome.github.io/Font-Awesome/