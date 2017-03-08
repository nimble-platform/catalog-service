/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.registration;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.utility.FileUtility;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.JAXBUtility;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 *
 * @author yildiray
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RegistrationServiceTest {

	@BeforeClass
	public static void startH2DB() {
		HibernateUtility.startH2DB();
	}

	@AfterClass
	public static void stopH2DB() {
		HibernateUtility.stopH2DB();
	}

	@Test
	public void addUserTest() {
		String personTypeXML = FileUtility.readFile("../ubl-data-model/src/test/resources/UBL-PersonFullDummy.xml");
		PersonType user = (PersonType) JAXBUtility.deserialize(personTypeXML, "eu.nimble.service.model.ubl.commonaggregatecomponents");
		RegistrationServiceImpl.getInstance().addUser(user);
	}
	
	@Test
	public void getUserTest() {
		PersonType user = RegistrationServiceImpl.getInstance().getUser("1387");
		System.out.println(" $$$ User name: " + user.getID());
	}
	
	@Test
	public void getUserListTest() {
		List<PersonType> users = RegistrationServiceImpl.getInstance().listUser();
		System.out.println(" $$$ User list size: " + users.size());
	}
	
}
