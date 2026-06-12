package com.xcodez.springaivoicecanvas.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcodez.springaivoicecanvas.model.*;
import com.xcodez.springaivoicecanvas.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DiagramVersionService {

    private static final Logger log = LoggerFactory.getLogger(DiagramVersionService.class);

    private final DiagramVersionRepository versionRepo;
    private final ExecutionLogRepository logRepo;
    private final ObjectMapper objectMapper;

    public DiagramVersionService(DiagramVersionRepository versionRepo,
                                 ExecutionLogRepository logRepo,
                                 ObjectMapper objectMapper) {
        this.versionRepo = versionRepo;
        this.logRepo = logRepo;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建新版本（全量快照）
     */
    @Transactional
    public DiagramVersion createVersion(String conversationId, String parentVersionId,
                                         String command, String summary,
                                         Object diagramObj, List<Map<String, Object>> logs) {
        String versionId = UUID.randomUUID().toString();
        String diagramJson = serializeQuietly(diagramObj);

        DiagramVersion v = new DiagramVersion(versionId, conversationId, parentVersionId,
                command, summary, diagramJson, null);
        v = versionRepo.save(v);

        // 保存执行日志
        if (logs != null) {
            for (int i = 0; i < logs.size(); i++) {
                Map<String, Object> entry = logs.get(i);
                ExecutionLog el = new ExecutionLog(
                        versionId,
                        (String) entry.getOrDefault("stepName", ""),
                        (String) entry.getOrDefault("content", ""),
                        i,
                        (String) entry.getOrDefault("level", "info")
                );
                logRepo.save(el);
            }
        }

        return v;
    }

    /**
     * 获取版本树（排好序，含父子关系）
     */
    public List<Map<String, Object>> getVersionTree(String conversationId) {
        List<DiagramVersion> versions = versionRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
        Map<String, List<DiagramVersion>> childrenMap = new HashMap<>();
        List<DiagramVersion> roots = new ArrayList<>();

        for (DiagramVersion v : versions) {
            if (v.getParentVersionId() == null) {
                roots.add(v);
            } else {
                childrenMap.computeIfAbsent(v.getParentVersionId(), k -> new ArrayList<>()).add(v);
            }
        }

        List<Map<String, Object>> tree = new ArrayList<>();
        for (DiagramVersion root : roots) {
            tree.add(buildNode(root, childrenMap));
        }
        return tree;
    }

    private Map<String, Object> buildNode(DiagramVersion v, Map<String, List<DiagramVersion>> childrenMap) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", v.getId());
        node.put("parentVersionId", v.getParentVersionId());
        node.put("command", v.getCommand());
        node.put("summary", v.getSummary());
        node.put("createdAt", v.getCreatedAt());

        List<DiagramVersion> children = childrenMap.getOrDefault(v.getId(), List.of());
        List<Map<String, Object>> childNodes = new ArrayList<>();
        for (DiagramVersion child : children) {
            childNodes.add(buildNode(child, childrenMap));
        }
        node.put("children", childNodes);
        return node;
    }

    /**
     * 获取单个版本的详情（含执行日志）
     */
    public Map<String, Object> getVersionDetail(String versionId) {
        DiagramVersion v = versionRepo.findById(versionId).orElse(null);
        if (v == null) return Map.of("error", "版本不存在");

        List<ExecutionLog> logs = logRepo.findByVersionIdOrderBySortOrderAsc(versionId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", v.getId());
        result.put("conversationId", v.getConversationId());
        result.put("parentVersionId", v.getParentVersionId());
        result.put("command", v.getCommand());
        result.put("summary", v.getSummary());
        result.put("diagramJson", v.getDiagramJson());
        result.put("createdAt", v.getCreatedAt());

        List<Map<String, Object>> logList = new ArrayList<>();
        for (ExecutionLog l : logs) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", l.getId());
            entry.put("stepName", l.getStepName());
            entry.put("content", l.getContent());
            entry.put("level", l.getLevel());
            logList.add(entry);
        }
        result.put("logs", logList);
        return result;
    }

    /**
     * 从指定版本获取完整图数据（用于恢复画布）
     */
    public DiagramVersion getVersion(String versionId) {
        return versionRepo.findById(versionId).orElse(null);
    }

    private String serializeQuietly(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("序列化 diagramJson 失败: {}", e.getMessage());
            return null;
        }
    }
}
