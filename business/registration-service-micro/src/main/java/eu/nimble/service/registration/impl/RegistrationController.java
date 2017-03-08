package eu.nimble.service.registration.impl;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.service.registration.RegistrationService;
import eu.nimble.service.registration.RegistrationServiceImpl;
import eu.nimble.utility.JAXBUtility;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class RegistrationController {

	private static Logger log = LoggerFactory
		.getLogger(RegistrationController.class);

	private RegistrationService service = RegistrationServiceImpl.getInstance();

	@RequestMapping(value = "/user/{ID}",
		produces = {"application/json"},
		method = RequestMethod.GET)
	public ResponseEntity<PersonType> getUser(@PathVariable String ID) {
		PersonType user = service.getUser(ID);
		return ResponseEntity.ok(user);
	}

	@RequestMapping(value = "/user",
		consumes = {"application/json"},
		method = RequestMethod.POST)
	public ResponseEntity<Void> addUser(@RequestBody PersonType person) {
		if (person == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		} else {
			/*String personTypeXML = JAXBUtility.serialize(person, "PersonType");
			log.info(" $$$ XML Representation: " + personTypeXML);*/
			service.addUser(person);
			return ResponseEntity.ok(null);
		}
	}

	@RequestMapping(value = "/user",
		consumes = {"application/json"},
		method = RequestMethod.PUT)
	public ResponseEntity<Void> updateUser(@RequestBody PersonType person) {
		if (person == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		} else {
			service.updateUser(person);
			return ResponseEntity.ok(null);
		}
	}

	@RequestMapping(value = "/user",
		produces = {"application/json"},
		method = RequestMethod.DELETE)
	public ResponseEntity<Void> deleteUser(@PathVariable String ID) {
		service.deleteUser(ID);
		return ResponseEntity.ok(null);
	}

	@RequestMapping(value = "/user",
		produces = {"application/json"},
		method = RequestMethod.GET)
	public ResponseEntity<List<PersonType>> getUsers() {
		List<PersonType> userList = service.listUser();
		return ResponseEntity.ok(userList);
	}
}
