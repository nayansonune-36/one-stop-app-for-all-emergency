package main

import (
	_ "embed"
	"os"
	"regexp"
	"strings"
)

//go:embed prompts/firstaid.txt
var embeddedSystemPrompt string

// GetSystemPrompt loads the system prompt either from the filesystem or embedded fallback.
func GetSystemPrompt(lang string) string {
	basePrompt := embeddedSystemPrompt
	if basePrompt == "" {
		if data, err := os.ReadFile("prompts/firstaid.txt"); err == nil {
			basePrompt = string(data)
		} else if data, err := os.ReadFile("backend/prompts/firstaid.txt"); err == nil {
			basePrompt = string(data)
		}
	}

	langInstruction := "English"
	if strings.ToLower(strings.TrimSpace(lang)) == "hi" {
		langInstruction = "Hindi (हिन्दी) in simple, conversational Devanagari script"
	}

	prompt := strings.ReplaceAll(basePrompt, "{{LANG}}", langInstruction)
	return prompt + "\n\nTarget Response Language: " + langInstruction
}

var criticalRegex = regexp.MustCompile(`(?im)^\s*[*_]*CRITICAL[*_]*\s*:\s*[*_]*(true|false)[*_.]*\s*$`)

// ParseCritical extracts and strips "CRITICAL: true|false" from the model reply.
func ParseCritical(rawReply string) (cleanReply string, isCritical bool) {
	lines := strings.Split(rawReply, "\n")
	var remainingLines []string
	found := false

	// Scan lines from bottom to top to find the final CRITICAL marker
	for i := len(lines) - 1; i >= 0; i-- {
		line := lines[i]
		if !found {
			matches := criticalRegex.FindStringSubmatch(line)
			if len(matches) > 1 {
				isCritical = strings.EqualFold(matches[1], "true")
				found = true
				continue
			}
		}
		remainingLines = append([]string{line}, remainingLines...)
	}

	cleanReply = strings.TrimSpace(strings.Join(remainingLines, "\n"))
	return cleanReply, isCritical
}

// CriticalKeywords contains high-severity life-threatening emergency terms.
var CriticalKeywords = []string{
	// English keywords
	"not breathing",
	"unconscious",
	"severe bleeding",
	"heavy bleeding",
	"massive bleeding",
	"choking",
	"chest pain",
	"snake bite",
	"snakebite",
	"cardiac arrest",
	"heart attack",
	"no pulse",
	"stopped breathing",
	"cannot breathe",
	"can't breathe",
	"difficulty breathing",
	"head injury",
	"anaphylaxis",
	"seizure",
	"convulsion",
	"poisoning",
	"drowning",
	"electric shock",

	// Hindi keywords (Devanagari)
	"सांस नहीं",
	"साँस नहीं",
	"बेहोश",
	"अचेत",
	"खून बह",
	"दम घुट",
	"सीने में दर्द",
	"छाती में दर्द",
	"सांप",
	"साँप",
	"हार्ट अटैक",
	"नाड़ी नहीं",
	"दौरा",
	"जहर",
	"विषाक्त",
	"सिर में चोट",
	"बिजली का झटका",
	"डूब",

	// Romanized Hindi / Hinglish keywords
	"saans nahi",
	"sans nahi",
	"behosh",
	"behos",
	"khoon beh",
	"seene me dard",
	"saamp",
}

// MatchesCriticalKeywords checks if the provided text contains any critical indicators.
func MatchesCriticalKeywords(text string) bool {
	lower := strings.ToLower(text)
	for _, kw := range CriticalKeywords {
		if strings.Contains(lower, strings.ToLower(kw)) {
			return true
		}
	}
	return false
}

// ProcessTriageResponse combines model output critical parsing with keyword fallback.
func ProcessTriageResponse(userMessages []ChatMessage, rawReply string) (cleanReply string, isCritical bool) {
	cleanReply, modelCritical := ParseCritical(rawReply)

	// Fallback check on user input messages
	keywordCritical := false
	for _, msg := range userMessages {
		if MatchesCriticalKeywords(msg.Content) {
			keywordCritical = true
			break
		}
	}

	// Also check raw reply itself for safety
	if !keywordCritical && MatchesCriticalKeywords(cleanReply) {
		keywordCritical = true
	}

	isCritical = modelCritical || keywordCritical
	return cleanReply, isCritical
}
