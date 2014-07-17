<a href="http://projectdanube.org/" target="_blank"><img src="http://projectdanube.github.com/xdi2/images/projectdanube_logo.png" align="right"></a>
<img src="http://projectdanube.github.com/xdi2/images/logo64.png"><br>

This is a project to add functionality to an XDI2 server to integrate with a local filesystem, using the XDI2
contributor mechanism. The directories and information about files are mapping to an XDI graph structure,
using content-based identifiers. The file contents are not mapped to XDI, but through a servlet filter access
to files can be governed by XDI link contracts.

### Information

* [Server Configuration Example](https://github.com/projectdanube/xdi2-mongodb/wiki/Server%20Configuration%20Example)

### How to build

First, you need to build the main [XDI2](http://github.com/projectdanube/xdi2) project.

After that, just run

    mvn clean install

To build all components.

### How to run

    mvn jetty:run

Then access the web interface at

	http://localhost:9130/

Or access the server's status page at

	http://localhost:9130/xdi

Or use an XDI client to send XDI messages to

    http://localhost:9130/xdi/graph

### How to build as XDI2 plugin

Run

    mvn clean install package -P xdi2-plugin

### Community

Google Group: http://groups.google.com/group/xdi2
