package eu.nimble.service.registration;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by suat on 23-Feb-17.
 */
public class RegistrationServiceImpl implements RegistrationService {

	private static final Logger logger = LoggerFactory.getLogger(RegistrationServiceImpl.class);
	private static RegistrationService instance = null;

	public static RegistrationService getInstance() {
		if (instance == null) {
			return new RegistrationServiceImpl();
		} else {
			return instance;
		}
	}

	private RegistrationServiceImpl() {
	}

	@Override
	public void addUser(PersonType user) {
		HibernateUtility.getInstance().persist(user);
	}

	// The users's schemeID attribute value is 'NIMBLE'
	@Override
	public PersonType getUser(String id) {
		String query = "SELECT person FROM PersonType person "
			+ " JOIN FETCH person.ID person_id "
			+ " WHERE person_id.value = '" + id + "' AND "
			+ " person_id.schemeID = 'NIMBLE'";
		List<PersonType> resultSet = (List<PersonType>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
			.loadAll(query);
		
		return resultSet.get(0);
	}

	@Override
	public void deleteUser(String id) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void updateUser(PersonType user) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public List<PersonType> listUser() {
		String query = "SELECT person FROM PersonType person "
			+ " JOIN FETCH person.ID person_id "
			+ " WHERE "
			+ " person_id.schemeID = 'NIMBLE'";
		List<PersonType> resultSet = (List<PersonType>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
			.loadAll(query);
		
		return resultSet;
	}
}
