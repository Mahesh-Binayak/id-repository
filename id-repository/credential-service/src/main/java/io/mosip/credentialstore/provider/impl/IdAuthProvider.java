package io.mosip.credentialstore.provider.impl;

import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;


import io.mosip.credentialstore.constants.CredentialConstants;
import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.dto.EncryptZkResponseDto;
import io.mosip.credentialstore.dto.PolicyDetailResponseDto;
import io.mosip.credentialstore.dto.ShareableAttribute;
import io.mosip.credentialstore.dto.ZkDataAttribute;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.exception.DataEncryptionFailureException;
import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.util.EncryptionUtil;
import io.mosip.credentialstore.util.JsonUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.kernel.core.util.DateUtils;




// TODO: Auto-generated Javadoc
/**
 * The Class IdAuthProvider.
 * 
 * @author Sowmya
 */
@Component
public class IdAuthProvider implements CredentialProvider {
	
	
	/** The utilities. */
	@Autowired
    Utilities utilities;	
	
	/** The env. */
	@Autowired
	Environment env;

	
	/** The Constant MODULO_VALUE. */
	public static final String MODULO_VALUE = "mosip.credential.service.modulo-value";
	
	/** The Constant DEMO_ENCRYPTED_RANDOM_KEY. */
	public static final String DEMO_ENCRYPTED_RANDOM_KEY = "demoEncryptedRandomKey";
	
	/** The Constant DEMO_ENCRYPTED_RANDOM_INDEX. */
	public static final String DEMO_ENCRYPTED_RANDOM_INDEX = "demoRankomKeyIndex";
	
	/** The Constant BIO_ENCRYPTED_RANDOM_KEY. */
	public static final String BIO_ENCRYPTED_RANDOM_KEY = "bioEncryptedRandomKey";
	
	/** The Constant BIO_ENCRYPTED_RANDOM_INDEX. */
	public static final String BIO_ENCRYPTED_RANDOM_INDEX = "bioRankomKeyIndex";
	
	/** The Constant DATETIME_PATTERN. */
	public static final String DATETIME_PATTERN = "mosip.credential.service.datetime.pattern";

	/** The encryption util. */
	@Autowired
	EncryptionUtil encryptionUtil;

	/* (non-Javadoc)
	 * @see io.mosip.credentialstore.provider.CredentialProvider#getFormattedCredentialData(java.util.Map, io.mosip.idrepository.core.dto.CredentialServiceRequestDto, java.util.Map)
	 */
	@Override
	public DataProviderResponse getFormattedCredentialData(	Map<String,Boolean> encryptMap,
			CredentialServiceRequestDto credentialServiceRequestDto, Map<String, Object> sharableAttributeMap)
			throws CredentialFormatterException {
		DataProviderResponse dataProviderResponse=new DataProviderResponse();
		try {
			
			List<ZkDataAttribute> bioZkDataAttributes=new ArrayList<>();
			
			List<ZkDataAttribute> demoZkDataAttributes=new ArrayList<>();
            Map<String, Object> formattedMap=new HashMap<>();
              
		 for (Map.Entry<String,Object> entry : sharableAttributeMap.entrySet()) {
			    String key=entry.getKey();
				Object value = entry.getValue();
				if (encryptMap.get(key)) {
					ZkDataAttribute zkDataAttribute=new ZkDataAttribute();
					zkDataAttribute.setIdentifier(key);
					zkDataAttribute.setValue(value.toString());
					if(key.equalsIgnoreCase(CredentialConstants.FACE) ||key.equalsIgnoreCase(CredentialConstants.IRIS) ||key.equalsIgnoreCase(CredentialConstants.FINGER)) {
						bioZkDataAttributes.add(zkDataAttribute);
					}else {
                      demoZkDataAttributes.add(zkDataAttribute);
					}
						
				} else {
					formattedMap.put(key, value);
				}
				

		}
		    EncryptZkResponseDto demoEncryptZkResponseDto=  encryptionUtil.encryptDataWithZK(credentialServiceRequestDto.getId(), demoZkDataAttributes);
		    EncryptZkResponseDto bioEncryptZkResponseDto=  encryptionUtil.encryptDataWithZK(credentialServiceRequestDto.getId(), bioZkDataAttributes);
			Map<String,Object> additionalData=credentialServiceRequestDto.getAdditionalData();
		    addToFormatter(demoEncryptZkResponseDto,formattedMap);
		    addToFormatter(bioEncryptZkResponseDto,formattedMap);
		    String credentialId = utilities.generateId();
		    additionalData.put(DEMO_ENCRYPTED_RANDOM_KEY, demoEncryptZkResponseDto.getEncryptedRandomKey());
		    additionalData.put(DEMO_ENCRYPTED_RANDOM_INDEX, demoEncryptZkResponseDto.getRankomKeyIndex());
		    additionalData.put(BIO_ENCRYPTED_RANDOM_KEY, demoEncryptZkResponseDto.getEncryptedRandomKey());
		    additionalData.put(BIO_ENCRYPTED_RANDOM_INDEX, demoEncryptZkResponseDto.getRankomKeyIndex());
		    additionalData.put("CREDENTIALID", credentialId);
		    credentialServiceRequestDto.setAdditionalData(additionalData);
		    String data = JsonUtil.objectMapperObjectToJson(formattedMap);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
			dataProviderResponse.setIssuanceDate(localdatetime);
			dataProviderResponse.setFormattedData(data.getBytes());
			dataProviderResponse.setCredentialId(credentialId);
			return dataProviderResponse;
		} catch (IOException e) {
			throw new CredentialFormatterException(e);
		} catch (DataEncryptionFailureException e) {
			throw new CredentialFormatterException(e);
		} catch (ApiNotAccessibleException e) {
			throw new CredentialFormatterException(e);
		}
		


	}
	
	/**
	 * Adds the to formatter.
	 *
	 * @param demoEncryptZkResponseDto the demo encrypt zk response dto
	 * @param formattedMap the formatted map
	 */
	private void addToFormatter(EncryptZkResponseDto demoEncryptZkResponseDto, Map<String, Object> formattedMap) {
		List<ZkDataAttribute> zkDataAttributes= demoEncryptZkResponseDto.getZkDataAttributes();
		for(ZkDataAttribute attribute:zkDataAttributes) {
			formattedMap.put(attribute.getIdentifier(), attribute.getValue());
		}

		
	}

}
