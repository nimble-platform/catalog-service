package eu.nimble.service.catalogue.model.statistics;

public class ProductAndServiceStatistics {

	private int totalServices;

	private int totalProducts;

	private ProductAndServiceStatistics() {
	}

	public ProductAndServiceStatistics(int totalServices, int totalProducts) {
		this.totalServices = totalServices;
		this.totalProducts = totalProducts;
	}

	public int getTotalServices() {
		return totalServices;
	}

	public int getTotalProducts() {
		return totalProducts;
	}
}
