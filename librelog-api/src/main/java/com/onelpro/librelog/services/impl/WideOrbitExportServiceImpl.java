package com.onelpro.librelog.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onelpro.librelog.dto.WideOrbitExportDTO;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.AutomationCommand;
import com.onelpro.librelog.models.BreakStructure;
import com.onelpro.librelog.models.Channel;
import com.onelpro.librelog.models.ClockTemplate;
import com.onelpro.librelog.models.FixedAsset;
import com.onelpro.librelog.repositories.AutomationCommandRepository;
import com.onelpro.librelog.repositories.BreakStructureRepository;
import com.onelpro.librelog.repositories.ClockTemplateRepository;
import com.onelpro.librelog.repositories.FixedAssetRepository;
import com.onelpro.librelog.services.WideOrbitExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of WideOrbit export service.
 * Converts internal clock template format to WideOrbit-compatible format.
 */
@Service
public class WideOrbitExportServiceImpl implements WideOrbitExportService {

	private static final Logger logger = LoggerFactory.getLogger(WideOrbitExportServiceImpl.class);

	private final ClockTemplateRepository clockTemplateRepository;
	private final BreakStructureRepository breakStructureRepository;
	private final FixedAssetRepository fixedAssetRepository;
	private final AutomationCommandRepository automationCommandRepository;
	private final ObjectMapper objectMapper;

	public WideOrbitExportServiceImpl(
			ClockTemplateRepository clockTemplateRepository,
			BreakStructureRepository breakStructureRepository,
			FixedAssetRepository fixedAssetRepository,
			AutomationCommandRepository automationCommandRepository,
			ObjectProvider<ObjectMapper> objectMapperProvider) {
		this.clockTemplateRepository = clockTemplateRepository;
		this.breakStructureRepository = breakStructureRepository;
		this.fixedAssetRepository = fixedAssetRepository;
		this.automationCommandRepository = automationCommandRepository;
		this.objectMapper = objectMapperProvider.getIfAvailable(() -> new ObjectMapper());
	}

	@Override
	public WideOrbitExportDTO exportClock(UUID clockTemplateId) {
		logger.info("Exporting clock template {} to WideOrbit format", clockTemplateId);

		ClockTemplate clockTemplate = clockTemplateRepository.findById(clockTemplateId)
				.orElseThrow(() -> new NotFoundException("Clock template not found with ID: " + clockTemplateId));

		List<WideOrbitExportDTO.WideOrbitElementDTO> elements = new ArrayList<>();

		// Add break structures
		List<BreakStructure> breaks = breakStructureRepository.findByClockTemplateId(clockTemplateId);
		breaks.forEach(b -> {
			WideOrbitExportDTO.WideOrbitElementDTO element = WideOrbitExportDTO.WideOrbitElementDTO.builder()
					.type("BREAK")
					.name(b.getName())
					.startTime(b.getStartTime())
					.durationSeconds(b.getDurationSeconds())
					.assetType(b.getAssetType() != null ? b.getAssetType().name() : null)
					.musicCategory(b.getMusicCategory() != null ? b.getMusicCategory().name() : null)
					.showSegmentName(b.getShowSegmentName())
					.timingType(b.getTimingType() != null ? b.getTimingType().name() : null)
					.transitionCode(b.getTransitionCode() != null ? b.getTransitionCode().name() : null)
					.availType(b.getAvailType() != null ? b.getAvailType().getName() : null)
					.build();
			elements.add(element);
		});

		// Add fixed assets
		List<FixedAsset> fixedAssets = fixedAssetRepository.findByClockTemplateId(clockTemplateId);
		fixedAssets.forEach(fa -> {
			WideOrbitExportDTO.WideOrbitElementDTO element = WideOrbitExportDTO.WideOrbitElementDTO.builder()
					.type("FIXED_ASSET")
					.name(fa.getName())
					.startTime(fa.getStartTime())
					.assetType(fa.getAssetType() != null ? fa.getAssetType().name() : null)
					.musicCategory(fa.getMusicCategory() != null ? fa.getMusicCategory().name() : null)
					.showSegmentName(fa.getShowSegmentName())
					.assetIdentifier(fa.getAssetIdentifier())
					.timingType(fa.getTimingType() != null ? fa.getTimingType().name() : null)
					.build();
			elements.add(element);
		});

		// Add automation commands
		List<AutomationCommand> commands = automationCommandRepository.findByClockTemplateId(clockTemplateId);
		commands.forEach(ac -> {
			WideOrbitExportDTO.WideOrbitElementDTO element = WideOrbitExportDTO.WideOrbitElementDTO.builder()
					.type("AUTOMATION_COMMAND")
					.name(ac.getCommandType().name())
					.startTime(ac.getTriggerTime())
					.commandType(ac.getCommandType().name())
					.parameters(ac.getParameters())
					.build();
			elements.add(element);
		});

		// Sort elements by start time
		elements.sort(Comparator.comparing(WideOrbitExportDTO.WideOrbitElementDTO::getStartTime));

		Channel channel = clockTemplate.getChannel();
		return WideOrbitExportDTO.builder()
				.id(clockTemplate.getId())
				.name(clockTemplate.getName())
				.description(clockTemplate.getDescription())
				.channelName(channel != null ? channel.getName() : null)
				.stationCallSign(channel != null && channel.getStation() != null ? channel.getStation().getCallSign() : null)
				.elements(elements)
				.build();
	}

	@Override
	public String exportClockToXml(UUID clockTemplateId) {
		logger.info("Exporting clock template {} to WideOrbit XML format", clockTemplateId);
		WideOrbitExportDTO exportDTO = exportClock(clockTemplateId);
		
		// Simple XML generation (WideOrbit may have specific XML schema requirements)
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xml.append("<ClockTemplate>\n");
		xml.append("  <Id>").append(exportDTO.getId()).append("</Id>\n");
		xml.append("  <Name>").append(escapeXml(exportDTO.getName())).append("</Name>\n");
		xml.append("  <Description>").append(escapeXml(exportDTO.getDescription())).append("</Description>\n");
		xml.append("  <ChannelName>").append(escapeXml(exportDTO.getChannelName())).append("</ChannelName>\n");
		xml.append("  <StationCallSign>").append(escapeXml(exportDTO.getStationCallSign())).append("</StationCallSign>\n");
		xml.append("  <Elements>\n");
		
		for (WideOrbitExportDTO.WideOrbitElementDTO element : exportDTO.getElements()) {
			xml.append("    <Element>\n");
			xml.append("      <Type>").append(escapeXml(element.getType())).append("</Type>\n");
			xml.append("      <Name>").append(escapeXml(element.getName())).append("</Name>\n");
			xml.append("      <StartTime>").append(element.getStartTime()).append("</StartTime>\n");
			if (element.getDurationSeconds() != null) {
				xml.append("      <DurationSeconds>").append(element.getDurationSeconds()).append("</DurationSeconds>\n");
			}
			if (element.getAssetType() != null) {
				xml.append("      <AssetType>").append(escapeXml(element.getAssetType())).append("</AssetType>\n");
			}
			if (element.getMusicCategory() != null) {
				xml.append("      <MusicCategory>").append(escapeXml(element.getMusicCategory())).append("</MusicCategory>\n");
			}
			if (element.getShowSegmentName() != null) {
				xml.append("      <ShowSegmentName>").append(escapeXml(element.getShowSegmentName())).append("</ShowSegmentName>\n");
			}
			if (element.getAssetIdentifier() != null) {
				xml.append("      <AssetIdentifier>").append(escapeXml(element.getAssetIdentifier())).append("</AssetIdentifier>\n");
			}
			if (element.getCommandType() != null) {
				xml.append("      <CommandType>").append(escapeXml(element.getCommandType())).append("</CommandType>\n");
			}
			if (element.getTimingType() != null) {
				xml.append("      <TimingType>").append(escapeXml(element.getTimingType())).append("</TimingType>\n");
			}
			if (element.getTransitionCode() != null) {
				xml.append("      <TransitionCode>").append(escapeXml(element.getTransitionCode())).append("</TransitionCode>\n");
			}
			if (element.getAvailType() != null) {
				xml.append("      <AvailType>").append(escapeXml(element.getAvailType())).append("</AvailType>\n");
			}
			xml.append("    </Element>\n");
		}
		
		xml.append("  </Elements>\n");
		xml.append("</ClockTemplate>\n");
		
		return xml.toString();
	}

	@Override
	public String exportClockToJson(UUID clockTemplateId) {
		logger.info("Exporting clock template {} to WideOrbit JSON format", clockTemplateId);
		try {
			WideOrbitExportDTO exportDTO = exportClock(clockTemplateId);
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportDTO);
		} catch (Exception e) {
			logger.error("Error serializing clock template to JSON: {}", e.getMessage());
			throw new RuntimeException("Failed to export clock to JSON: " + e.getMessage());
		}
	}

	private String escapeXml(String str) {
		if (str == null) {
			return "";
		}
		return str.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;");
	}
}

