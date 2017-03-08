package eu.nimble.service.registration.rest;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.service.registration.RegistrationService;
import eu.nimble.service.registration.RegistrationServiceImpl;
import eu.nimble.utility.JAXBUtility;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class RegistrationServiceREST {

	private static Logger log = LoggerFactory
		.getLogger(RegistrationServiceREST.class);

	private RegistrationService service = RegistrationServiceImpl.getInstance();

	@GET
	@Path("ping")
	public Response check() {
		// localhost:6565/registration/ping
		log.info("RESTful Service {} is running ==> ping", this.getClass().getName());
		String output = "Server check : OK, ";
		output += "received ping on " + new Date().toString();
		return Response.status(200).entity(output).build();
	}

	@GET
	@Path("user/{ID}")
	@Produces(MediaType.APPLICATION_JSON)
	public PersonType getUser(@PathParam("ID") String ID) {
		PersonType user = service.getUser(ID);
		return user;
	}

	@POST
	@Path("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addUser(PersonType person) {
		//String personTypeXML = JAXBUtility.serialize(person, "PersonType");
		//log.info(" $$$ XML Representation: " + personTypeXML);
		service.addUser(person);
		return Response.status(200).entity("success").build();
	}

	@PUT
	@Path("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateUser(PersonType person) {
		service.updateUser(person);
		return Response.status(200).entity("success").build();
	}

	@Path("user")
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteUser(@QueryParam("ID") String ID) {
		service.deleteUser(ID);
		return Response.status(200).entity("success").build();
	}

	@Path("user")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<PersonType> listUser() {
		List<PersonType> userList = service.listUser();
		return userList;
		//return Response.serverError().build();
	}
}
