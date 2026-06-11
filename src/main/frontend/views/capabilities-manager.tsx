import React, { useEffect, useMemo, useRef, useState } from 'react';

type NodeType = 'root' | 'capability' | 'activity' | 'folder' | 'use-case';

type DocNode = {
  id: string;
  name: string;
  type: NodeType;
  relativePath: string;
  repositoryName: string;
  repositoryUrl: string;
  children: DocNode[];
  useCase: UseCaseDetails | null;
};

type UseCaseDetails = {
  repositoryName: string;
  repositoryUrl: string;
  capabilityId: string;
  activityPath: string;
  activityIds: string[];
  useCaseId: string;
  useCasePath: string;
  relativePath: string;
  ucMarkdown: string;
  featureText: string;
  scenarios: ScenarioBlock[];
};

type ScenarioBlock = {
  id: string;
  name: string;
  text: string;
};

type RepositorySummary = {
  name: string;
  url: string;
  checkoutPath: string;
  source: string;
};

type CapabilityTreeResponse = {
  root: DocNode;
  useCases: UseCaseDetails[];
  repositories: RepositorySummary[];
};

export default function CapabilitiesManager() {
  const [tree, setTree] = useState<CapabilityTreeResponse | null>(null);
  const [activeCapabilityId, setActiveCapabilityId] = useState('');
  const [selectedUseCaseKey, setSelectedUseCaseKey] = useState('');
  const [expandedNodeIds, setExpandedNodeIds] = useState<Set<string>>(new Set());
  const [expandedScenarioPaths, setExpandedScenarioPaths] = useState<Set<string>>(new Set());
  const [epicUseCase, setEpicUseCase] = useState<UseCaseDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let ignore = false;
    requestJson<CapabilityTreeResponse>('/api/capabilities/tree')
      .then((response) => {
        if (ignore) {
          return;
        }
        setTree(response);
        const firstCapability = response.root.children.find((node) => node.type === 'capability');
        const firstUseCase = response.useCases[0];
        setActiveCapabilityId(firstCapability?.id ?? '');
        setSelectedUseCaseKey(firstUseCase ? useCaseKey(firstUseCase) : '');
        setExpandedNodeIds(defaultExpandedNodeIds(response.root));
      })
      .catch((caught: unknown) => setError(errorMessage(caught)))
      .finally(() => {
        if (!ignore) {
          setLoading(false);
        }
      });
    return () => {
      ignore = true;
    };
  }, []);

  const capabilityNodes = useMemo(
    () => tree?.root.children.filter((node) => node.type === 'capability') ?? [],
    [tree],
  );

  const activeCapability = useMemo(() => {
    return capabilityNodes.find((node) => node.id === activeCapabilityId) ?? capabilityNodes[0] ?? null;
  }, [activeCapabilityId, capabilityNodes]);

  const selectedUseCase = useMemo(() => {
    return tree?.useCases.find((useCase) => useCaseKey(useCase) === selectedUseCaseKey) ?? null;
  }, [selectedUseCaseKey, tree]);

  const selectCapability = (capability: DocNode) => {
    setActiveCapabilityId(capability.id);
    setExpandedNodeIds((current) => {
      const next = new Set(current);
      next.add(capability.id);
      return next;
    });
    const firstUseCase = firstUseCaseInNode(capability);
    if (firstUseCase) {
      selectUseCase(firstUseCase);
    }
  };

  const selectUseCase = (useCase: UseCaseDetails) => {
    setSelectedUseCaseKey(useCaseKey(useCase));
  };

  const toggleNode = (nodeId: string) => {
    setExpandedNodeIds((current) => {
      const next = new Set(current);
      if (next.has(nodeId)) {
        next.delete(nodeId);
      } else {
        next.add(nodeId);
      }
      return next;
    });
  };

  const toggleScenarios = (useCase: UseCaseDetails) => {
    setExpandedScenarioPaths((current) => {
      const key = useCaseKey(useCase);
      const next = new Set(current);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  };

  if (loading) {
    return <Shell><div className="status-panel">Loading capabilities...</div></Shell>;
  }

  if (error && !tree) {
    return <Shell><div className="status-panel status-panel-error">{error}</div></Shell>;
  }

  return (
    <Shell>
      <header className="manager-header">
        <div>
          <p className="eyebrow">docs/capabilities</p>
          <h1>Capability Manager</h1>
        </div>
        <RepositoryList repositories={tree?.repositories ?? []} />
      </header>

      {capabilityNodes.length > 0 && (
        <nav className="capability-tabs" aria-label="Software capabilities">
          {capabilityNodes.map((capability) => (
            <button
              key={capability.id}
              type="button"
              className={capability.id === activeCapability?.id ? 'capability-tab active' : 'capability-tab'}
              onClick={() => selectCapability(capability)}
            >
              {capability.name}
            </button>
          ))}
        </nav>
      )}

      <main className="manager-grid">
        <aside className="tree-pane" aria-label="Capability tree">
          {activeCapability ? (
            <TreeNodeView
              node={activeCapability}
              depth={0}
              selectedUseCaseKey={selectedUseCaseKey}
              expandedNodeIds={expandedNodeIds}
              expandedScenarioPaths={expandedScenarioPaths}
              onToggleNode={toggleNode}
              onToggleScenarios={toggleScenarios}
              onSelectUseCase={selectUseCase}
              onCreateEpic={setEpicUseCase}
            />
          ) : (
            <div className="empty-pane">No capability folders found.</div>
          )}
        </aside>

        <section className="detail-pane" aria-label="Use-case workspace">
          {selectedUseCase ? (
            <>
              <div className="detail-toolbar">
                <div>
                  <p className="breadcrumb">{selectedUseCase.useCasePath}</p>
                  <h2>{selectedUseCase.useCaseId}</h2>
                  <p className="source-line">{selectedUseCase.repositoryName}</p>
                </div>
                <button type="button" className="primary-button" onClick={() => setEpicUseCase(selectedUseCase)}>
                  Create Epic
                </button>
              </div>

              {error && <div className="inline-error">{error}</div>}

              <MarkdownDocument markdown={selectedUseCase.ucMarkdown} />
            </>
          ) : (
            <div className="empty-pane">Select a use case.</div>
          )}
        </section>
      </main>

      {epicUseCase && (
        <EpicTemplateDialog useCase={epicUseCase} onClose={() => setEpicUseCase(null)} />
      )}
    </Shell>
  );
}

function Shell({ children }: { children: React.ReactNode }) {
  return <div className="capabilities-app">{children}</div>;
}

function RepositoryList({ repositories }: { repositories: RepositorySummary[] }) {
  if (repositories.length === 0) {
    return null;
  }
  return (
    <div className="repository-list" aria-label="Supported repositories">
      <p className="eyebrow">Repositories</p>
      {repositories.map((repository) => (
        <details className="repository-item" key={repository.name}>
          <summary>{repository.name}</summary>
          <a href={repository.url} target="_blank" rel="noreferrer">{repository.url}</a>
        </details>
      ))}
    </div>
  );
}

function TreeNodeView({
  node,
  depth,
  selectedUseCaseKey,
  expandedNodeIds,
  expandedScenarioPaths,
  onToggleNode,
  onToggleScenarios,
  onSelectUseCase,
  onCreateEpic,
}: {
  node: DocNode;
  depth: number;
  selectedUseCaseKey: string;
  expandedNodeIds: Set<string>;
  expandedScenarioPaths: Set<string>;
  onToggleNode: (nodeId: string) => void;
  onToggleScenarios: (useCase: UseCaseDetails) => void;
  onSelectUseCase: (useCase: UseCaseDetails) => void;
  onCreateEpic: (useCase: UseCaseDetails) => void;
}) {
  const hasChildren = node.children.length > 0;
  const isExpanded = expandedNodeIds.has(node.id);
  const useCase = node.useCase;
  const currentUseCaseKey = useCase ? useCaseKey(useCase) : '';
  const isSelected = currentUseCaseKey === selectedUseCaseKey;
  const scenariosExpanded = useCase ? expandedScenarioPaths.has(currentUseCaseKey) : false;

  return (
    <div className="tree-node">
      <div className={isSelected ? 'tree-row selected' : `tree-row ${node.type}`} style={{ paddingLeft: depth * 16 }}>
        {useCase ? (
          <button
            type="button"
            className="icon-button"
            aria-label={`Toggle scenarios for ${useCase.useCaseId}`}
            onClick={() => onToggleScenarios(useCase)}
          >
            {scenariosExpanded ? '-' : '+'}
          </button>
        ) : (
          <button
            type="button"
            className="icon-button"
            aria-label={`Toggle ${node.name}`}
            disabled={!hasChildren}
            onClick={() => onToggleNode(node.id)}
          >
            {hasChildren ? (isExpanded ? 'v' : '>') : ''}
          </button>
        )}

        <button
          type="button"
          className="node-name"
          onClick={() => {
            if (useCase) {
              onSelectUseCase(useCase);
            } else if (hasChildren) {
              onToggleNode(node.id);
            }
          }}
        >
          <span className="node-type">{node.type}</span>
          <span>{node.name}</span>
        </button>

        {useCase && (
          <div className="use-case-actions">
            <button type="button" className="primary-action-button" onClick={() => onCreateEpic(useCase)}>
              Create Epic
            </button>
            <button type="button" onClick={() => onSelectUseCase(useCase)}>View Use Case</button>
          </div>
        )}
      </div>

      {useCase && scenariosExpanded && (
        <div className="scenario-list" style={{ marginLeft: depth * 16 + 40 }}>
          {useCase.scenarios.length === 0 ? (
            <div className="scenario-row muted">No scenarios</div>
          ) : (
            useCase.scenarios.map((scenario) => (
              <div className="scenario-row" key={scenario.id}>
                <span>{scenario.name}</span>
              </div>
            ))
          )}
        </div>
      )}

      {hasChildren && isExpanded && node.children.map((child) => (
        <TreeNodeView
          key={child.id}
          node={child}
          depth={depth + 1}
          selectedUseCaseKey={selectedUseCaseKey}
          expandedNodeIds={expandedNodeIds}
          expandedScenarioPaths={expandedScenarioPaths}
          onToggleNode={onToggleNode}
          onToggleScenarios={onToggleScenarios}
          onSelectUseCase={onSelectUseCase}
          onCreateEpic={onCreateEpic}
        />
      ))}
    </div>
  );
}

function EpicTemplateDialog({ useCase, onClose }: { useCase: UseCaseDetails; onClose: () => void }) {
  const [templateText, setTemplateText] = useState(() => epicTemplate(useCase));
  const [copyStatus, setCopyStatus] = useState('');

  const copyTemplate = async () => {
    try {
      await navigator.clipboard.writeText(templateText);
      setCopyStatus('Copied');
    } catch {
      setCopyStatus('Select text to copy');
    }
  };

  return (
    <Dialog title="Create Epic" onClose={onClose}>
      <div className="dialog-summary">
        <span>{useCase.repositoryName}</span>
        <strong>{useCase.useCaseId}</strong>
      </div>
      <GherkinTextWindow value={templateText} onChange={setTemplateText} minRows={24} />
      <div className="dialog-actions">
        <span className="copy-status" aria-live="polite">{copyStatus}</span>
        <button type="button" className="secondary-button" onClick={copyTemplate}>Copy</button>
        <button type="button" className="primary-button" onClick={onClose}>Done</button>
      </div>
    </Dialog>
  );
}

function GherkinTextWindow({
  value,
  onChange,
  minRows,
}: {
  value: string;
  onChange: (value: string) => void;
  minRows: number;
}) {
  const highlightRef = useRef<HTMLPreElement | null>(null);
  const minHeight = `${Math.max(minRows, 8) * 1.48 + 2}rem`;

  return (
    <div className="gherkin-editor" style={{ minHeight }}>
      <pre ref={highlightRef} className="gherkin-highlight" aria-hidden="true">
        {highlightGherkin(value)}
      </pre>
      <textarea
        aria-label="Epic acceptance criteria template"
        spellCheck={false}
        value={value}
        onChange={(event) => onChange(event.currentTarget.value)}
        onScroll={(event) => {
          if (highlightRef.current) {
            highlightRef.current.scrollTop = event.currentTarget.scrollTop;
            highlightRef.current.scrollLeft = event.currentTarget.scrollLeft;
          }
        }}
      />
    </div>
  );
}

function highlightGherkin(value: string) {
  const lines = value.split('\n');
  return lines.map((line, index) => (
    <React.Fragment key={`${index}-${line}`}>
      {highlightLine(line)}
      {index < lines.length - 1 ? '\n' : null}
    </React.Fragment>
  ));
}

function highlightLine(line: string) {
  const metadata = line.match(/^(Use Case ID|Use Case Path|Github)(:)(.*)$/);
  if (metadata) {
    return (
      <>
        <span className="token-metadata">{metadata[1]}</span><span className="token-punctuation">{metadata[2]}</span>
        <span>{metadata[3]}</span>
      </>
    );
  }

  const footer = line.match(/^(Deleted Scenarios)(:?)$/);
  if (footer) {
    return <span className="token-feature">{line}</span>;
  }

  const keyword = line.match(/^(\s*)(Feature|Rule|Background|Scenario(?: Outline)?|Examples)(:.*)$/);
  if (keyword) {
    return (
      <>
        {keyword[1]}<span className="token-feature">{keyword[2]}</span><span className="token-punctuation">{keyword[3]}</span>
      </>
    );
  }

  const step = line.match(/^(\s*)(Given|When|Then|And|But)(\b.*)$/);
  if (step) {
    return (
      <>
        {step[1]}<span className="token-step">{step[2]}</span><span>{step[3]}</span>
      </>
    );
  }

  if (line.trimStart().startsWith('@')) {
    return <span className="token-tag">{line}</span>;
  }

  if (line.trimStart().startsWith('#')) {
    return <span className="token-comment">{line}</span>;
  }

  return <span>{line || ' '}</span>;
}

function MarkdownDocument({ markdown }: { markdown: string }) {
  const blocks = markdown.split('\n');
  return (
    <article className="markdown-document">
      {blocks.map((line, index) => {
        if (line.startsWith('# ')) {
          return <h1 key={index}>{line.slice(2)}</h1>;
        }
        if (line.startsWith('## ')) {
          return <h2 key={index}>{line.slice(3)}</h2>;
        }
        if (line.startsWith('### ')) {
          return <h3 key={index}>{line.slice(4)}</h3>;
        }
        if (line.startsWith('- ')) {
          return <p className="markdown-list-item" key={index}>{line}</p>;
        }
        if (line.startsWith('|')) {
          return <pre className="markdown-table" key={index}>{line}</pre>;
        }
        if (line.trim().length === 0) {
          return <div className="markdown-space" key={index} />;
        }
        return <p key={index}>{line}</p>;
      })}
    </article>
  );
}

function Dialog({
  title,
  children,
  onClose,
}: {
  title: string;
  children: React.ReactNode;
  onClose: () => void;
}) {
  return (
    <div className="dialog-backdrop" role="presentation">
      <section className="dialog" role="dialog" aria-modal="true" aria-label={title}>
        <div className="dialog-header">
          <h2>{title}</h2>
          <button type="button" className="icon-button" aria-label="Close" onClick={onClose}>X</button>
        </div>
        {children}
      </section>
    </div>
  );
}

async function requestJson<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, options);
  if (!response.ok) {
    let message = `${response.status} ${response.statusText}`;
    try {
      const body = await response.json();
      message = body.detail ?? body.message ?? body.title ?? message;
    } catch {
      message = await response.text();
    }
    throw new Error(message);
  }
  return response.json() as Promise<T>;
}

function defaultExpandedNodeIds(root: DocNode) {
  const ids = new Set<string>();
  const visit = (node: DocNode) => {
    if (node.type !== 'use-case') {
      ids.add(node.id);
    }
    node.children.forEach(visit);
  };
  visit(root);
  return ids;
}

function firstUseCaseInNode(node: DocNode): UseCaseDetails | null {
  if (node.useCase) {
    return node.useCase;
  }
  for (const child of node.children) {
    const useCase = firstUseCaseInNode(child);
    if (useCase) {
      return useCase;
    }
  }
  return null;
}

function epicTemplate(useCase: UseCaseDetails) {
  return [
    `Use Case ID: ${useCase.useCaseId}`,
    `Use Case Path: ${useCase.useCasePath}`,
    `Github: ${useCase.repositoryUrl}`,
    '',
    useCase.featureText.trimEnd(),
    '',
  ].join('\n');
}

function useCaseKey(useCase: UseCaseDetails) {
  return `${useCase.repositoryName}:${useCase.relativePath}`;
}

function errorMessage(caught: unknown) {
  return caught instanceof Error ? caught.message : 'Unexpected error';
}
