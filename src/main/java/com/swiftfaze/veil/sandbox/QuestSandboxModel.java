package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.quests.Quest;
import com.swiftfaze.veil.mods.ModLoader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class QuestSandboxModel {

    private final List<Quest> quests;

    public QuestSandboxModel() {
        this(ModLoader.load(Paths.get("mods")).getAllQuests());
    }

    public QuestSandboxModel(List<Quest> quests) {
        this.quests = quests;
    }

    public QuestSandboxModel(Path modsRoot) {
        this(ModLoader.load(modsRoot).getAllQuests());
    }

    public List<Quest> quests() {
        return quests;
    }

    public Quest findById(String id) {
        return quests.stream()
                .filter(q -> q.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown quest id: " + id));
    }
}
