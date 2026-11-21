export type WorkMode = "REMOTE" | "HYBRID" | "ONSITE";
export type AppStatus = "SAVED" | "APPLIED" | "INTERVIEW" | "OFFER" | "REJECTED";

export interface Vacancy {
  id: number;
  source: string;
  url: string;
  title: string;
  company: string;
  country?: string;
  city?: string;
  workMode: WorkMode;
  language: string;
  salaryMin?: number;
  salaryMax?: number;
  salaryCurrency?: string;
  skills: string[];
  direction?: string;
  publishedAt?: string;
  fetchedAt: string;
  prefilterPassed: boolean;
  prefilterReason?: string;
  matchScore?: number;
  matchSummary?: string;
  matchStrengths?: string[];
  matchGaps?: string[];
  matchAdvice?: string;
  scoredBy?: "AI" | "KEYWORDS";
  hidden: boolean;
  description?: string;
  applicationStatus?: AppStatus;
}

export interface Page<T> {
  content: T[];
  page?: { totalElements: number; totalPages: number; number: number; size: number };
  totalElements?: number;
  totalPages?: number;
}

export interface Application {
  id: number;
  vacancyId: number;
  title: string;
  company: string;
  country?: string;
  matchScore?: number;
  url: string;
  status: AppStatus;
  notes?: string;
  appliedAt?: string;
  statusChangedAt: string;
  needsFollowUp: boolean;
}

export interface CoverLetter {
  id: number;
  vacancyId: number;
  language: "EN" | "DE";
  version: number;
  body: string;
  generatedBy: "AI" | "TEMPLATE" | "MANUAL";
  createdAt: string;
}

export interface PromptTemplate {
  id: number;
  code: string;
  title: string;
  body: string;
  updatedAt: string;
}

export interface Settings {
  resumeText: string;
  directions: string;
  countries: string;
  keywords: string;
  stopWords: string;
  minScore: number;
  digestHour: number;
  remoteOnly: boolean;
}

export interface NamedCount {
  name: string;
  count: number;
  share: number;
}

export interface Insights {
  days: number;
  totalVacancies: number;
  relevantVacancies: number;
  avgScore?: number;
  topSkills: NamedCount[];
  topSkillsRelevant: NamedCount[];
  cvCoverage: { skill: string; demand: number; inCv: boolean }[];
  countries: NamedCount[];
  workModes: NamedCount[];
  languages: NamedCount[];
  directions: NamedCount[];
  salaries: { country: string; vacancies: number; avgMin: number; avgMax: number }[];
  weekly: { week: string; fetched: number; relevant: number; avgScore?: number }[];
  skillTrends: Record<string, string | number>[];
}

export interface Stats {
  weekly: { week: string; found: number; applied: number; interviews: number }[];
  funnel: Record<AppStatus, number>;
  aiSpentThisMonth: number;
  aiMonthlyBudget: number;
  aiCallsThisMonth: number;
  aiOffReason?: string;
  runs: {
    startedAt: string;
    trigger: string;
    status: string;
    fetched: number;
    new: number;
    duplicates: number;
    passed: number;
    scored: number;
    log: string;
  }[];
}

export interface Summary {
  processedLast30Days: number;
  relevantLast30Days: number;
  strongMatchesLast30Days: number;
  sources: string[];
  lastRunAt?: string;
  totalVacancies: number;
}

export interface MatchAnswer {
  score: number;
  summary: string;
  strengths: string[];
  gaps: string[];
  advice: string;
}

export interface AiTrace {
  candidates: { id: number; title: string; company: string; score: number }[];
  vacancyId?: number;
  model: string;
  instructions: string;
  cvCharacters: number;
  userPrompt?: string;
  schema: { name: string; type: string; description: string }[];
  answer?: MatchAnswer;
  keywordScore: number;
  keywordSummary?: string;
  call?: {
    at: string;
    model: string;
    inputTokens: number;
    outputTokens: number;
    cacheReadTokens: number;
    cacheWriteTokens: number;
    costUsd: number;
    durationMs?: number;
  };
  usage: {
    scoringCalls: number;
    avgInputTokens: number;
    avgOutputTokens: number;
    avgCacheReadTokens: number;
    avgCostUsd: number;
    totalCostUsd: number;
    costWithoutCacheUsd: number;
    cacheSavingPercent: number;
    letterCalls: number;
    avgLetterCostUsd: number;
    spentThisMonthUsd: number;
    monthlyLimitUsd: number;
  };
}
