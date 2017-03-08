/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.catalogue;

import eu.nimble.service.model.modaml.catalogue.TEXCatalogType;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.JAXBUtility;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author yildiray
 */
public class CatalogueServiceImpl implements CatalogueService {

	private static final Logger logger = LoggerFactory.getLogger(CatalogueServiceImpl.class);
	private static CatalogueService instance = null;

	public static CatalogueService getInstance() {
		if (instance == null) {
			return new CatalogueServiceImpl();
		} else {
			return instance;
		}
	}

	private CatalogueServiceImpl() {
	}

	@Override
	public void addCatalogue(CatalogueType catalogue) {
		HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(catalogue);
	}
	
	@Override
	public void addCatalogue(TEXCatalogType catalogue) {
		HibernateUtility.getInstance(Configuration.MODAML_PERSISTENCE_UNIT_NAME).persist(catalogue);
	}

	@Override
	public void addCatalogue(String xml, Configuration.Standard standard) {
		if (standard == Configuration.Standard.UBL) {
			CatalogueType catalogue = (CatalogueType) JAXBUtility.deserialize(xml, Configuration.UBL_CATALOGUE_PACKAGENAME);
			addCatalogue(catalogue);
		} else if (standard == Configuration.Standard.MODAML) {
			TEXCatalogType catalogue = (TEXCatalogType) JAXBUtility.deserialize(xml, Configuration.MODAML_CATALOGUE_PACKAGENAME);
			addCatalogue(catalogue);
		}
	}

	@Override
	public Object getCatalogueByUUID(String uuid, Configuration.Standard standard) {
		List resultSet = null;

		if (standard == Configuration.Standard.UBL) {
			String query = "SELECT catalogue FROM CatalogueType catalogue "
				+ " JOIN FETCH catalogue.UUID catalogue_uuid "
				+ " WHERE catalogue_uuid.value = '" + uuid + "'";
			resultSet = (List<CatalogueType>) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
				.loadAll(query);
		} else if (standard == Configuration.Standard.MODAML) {
			String query = "SELECT catalogue FROM TEXCatalogType catalogue "
				+ " JOIN FETCH catalogue.TCheader catalogue_header "
				+ " WHERE catalogue_header.msgID = '" + uuid + "'";
			resultSet = (List<TEXCatalogType>) HibernateUtility.getInstance(Configuration.MODAML_PERSISTENCE_UNIT_NAME)
				.loadAll(query);
		}

		if (resultSet.size() > 0) {
			return resultSet.get(0);
		}

		return null;
	}

	@Override
	public void deleteCatalogueByUUID(String uuid, Configuration.Standard standard) {
		if (standard == Configuration.Standard.UBL) {
			CatalogueType catalogue = (CatalogueType) getCatalogueByUUID(uuid, standard);
			HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(CatalogueType.class, catalogue.getHjid());
		} else if (standard == Configuration.Standard.MODAML) {
			TEXCatalogType catalogue = (TEXCatalogType) getCatalogueByUUID(uuid, standard);
			HibernateUtility.getInstance(Configuration.MODAML_PERSISTENCE_UNIT_NAME).delete(TEXCatalogType.class, catalogue.getHjid());
		}
	}
}
