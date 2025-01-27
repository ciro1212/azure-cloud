package com.chtrembl.petstoreapp.service;

/**
 * Implementation for service calls to the APIM/AKS
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import com.chtrembl.petstoreapp.model.Category;
import com.chtrembl.petstoreapp.model.ContainerEnvironment;
import com.chtrembl.petstoreapp.model.Pet;
import com.chtrembl.petstoreapp.model.User;

@Component
public class PetStoreServiceImpl implements PetStoreService {
	@Autowired
	private User sessionUser;

	@Autowired
	private ContainerEnvironment containerEnvironment;

	private WebClient webClient = null;

	@PostConstruct
	public void initialize() {
		this.webClient = WebClient.builder().baseUrl(this.containerEnvironment.getPetStoreServiceURL()).build();
	}

	@Override
	public Collection<Pet> getPets(String category) {
		List<Pet> pets = new ArrayList<Pet>();

		this.sessionUser.getTelemetryClient()
				.trackEvent(String.format("PetStoreApp %s is requesting to retrieve pets from the PetStoreService",
						this.sessionUser.getName()), this.sessionUser.getCustomEventProperties(), null);
		try {
			pets = this.webClient.get().uri("/v2/pet/findByStatus?status={status}", "available")
					.header("session-id", this.sessionUser.getSessionId()).accept(MediaType.APPLICATION_JSON)
					.header("Ocp-Apim-Subscription-Key", this.containerEnvironment.getPetStoreServiceSubscriptionKey())
					.header("Ocp-Apim-Trace", "true").retrieve()
					.bodyToMono(new ParameterizedTypeReference<List<Pet>>() {
					}).block();

			// use this for look up on details page, intentionally avoiding spring cache to
			// ensure service calls are made each
			// time to show Telemetry with APIM requests
			this.sessionUser.setPets(pets);

			// filter this specific request per category
			pets = pets.stream().filter(pet -> category.equals(pet.getCategory().getName()))
					.collect(Collectors.toList());
			return pets;
		} catch (WebClientException wce) {
			this.sessionUser.getTelemetryClient().trackException(wce);
			this.sessionUser.getTelemetryClient().trackEvent(
					String.format("PetStoreApp %s received %s, container host: %s", this.sessionUser.getName(),
							wce.getMessage(), this.containerEnvironment.getContainerHostName()));
			// little hack to visually show the error message within our Azure Pet Store
			// Reference Guide (Academic Tutorial)
			Pet pet = new Pet();
			pet.setName(wce.getMessage());
			pet.setPhotoURL("");
			pet.setCategory(new Category());
			pet.setId((long) 0);
			pets.add(pet);
		} catch (IllegalArgumentException iae) {
			// little hack to visually show the error message within our Azure Pet Store
			// Reference Guide (Academic Tutorial)
			Pet pet = new Pet();
			pet.setName("petstore.service.url:${PETSTORESERVICE_URL} needs to be enabled for this service to work"
					+ iae.getMessage());
			pet.setPhotoURL("");
			pet.setCategory(new Category());
			pet.setId((long) 0);
			pets.add(pet);
		}
		return pets;
	}
}
