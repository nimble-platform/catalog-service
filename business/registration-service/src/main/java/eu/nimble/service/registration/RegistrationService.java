package eu.nimble.service.registration;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import java.util.List;

public interface RegistrationService {

	public PersonType getUser(String id);

	public void deleteUser(String id);

	public void addUser(PersonType user);

	public void updateUser(PersonType user);

	public List<PersonType> listUser();
}
