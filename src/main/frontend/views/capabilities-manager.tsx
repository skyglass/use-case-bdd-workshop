import React, { useEffect, useMemo, useRef, useState } from 'react';

type NodeType = 'root' | 'capability' | 'activity' | 'folder' | 'use-case';
type PanelMode = 'submit' | 'view';

type DocNode = {
  id: string;
  name: string;
  type: NodeType;
  relativePath: string;
  children: DocNode[];
  useCase: UseCaseDetails | null;
};

type UseCaseDetails = {
  capabilityId: string;
  activityId: string;
  useCaseId: string;
  relativePath: string;
  ucMarkdown: string;
  featureText: string;
  scenarios: ScenarioBlock[];
  nextEpicName: string;
};

type ScenarioBlock = {
  id: string;
  name: string;
  text: string;
};

type CapabilityTreeResponse = {
  root: DocNode;
  useCases: UseCaseDetails[];
};

type ChangedFile = {
  path: string;
  content: string;
};

type MockPullRequestResponse = {
  id: string;
  title: string;
  branch: string;
  status: string;
  createdAt: string;
  submitted: Record<string, string>;
  changedFiles: ChangedFile[];
};

type PendingDeletion = {
  useCase: UseCaseDetails;
  scenario: ScenarioBlock;
  epicText: string;
};

type NewUseCaseDialogProps = {
  tree: CapabilityTreeResponse;
  onClose: () => void;
  onMockPullRequest: (result: MockPullRequestResponse) => void;
};

export default function CapabilitiesManager() {
  const [tree, setTree] = useState<CapabilityTreeResponse | null>(null);
  const [activeCapabilityId, setActiveCapabilityId] = useState('');
  const [selectedUseCasePath, setSelectedUseCasePath] = useState('');
  const [panelMode, setPanelMode] = useState<PanelMode>('submit');
  const [expandedNodeIds, setExpandedNodeIds] = useState<Set<string>>(new Set());
  const [expandedScenarioPaths, setExpandedScenarioPaths] = useState<Set<string>>(new Set());
  const [editorText, setEditorText] = useState('');
  const [pendingDeletion, setPendingDeletion] = useState<PendingDeletion | null>(null);
  const [mockPullRequest, setMockPullRequest] = useState<MockPullRequestResponse | null>(null);
  const [newUseCaseOpen, setNewUseCaseOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

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
        setSelectedUseCasePath(firstUseCase?.relativePath ?? '');
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
    return tree?.useCases.find((useCase) => useCase.relativePath === selectedUseCasePath) ?? null;
  }, [selectedUseCasePath, tree]);

  useEffect(() => {
    if (selectedUseCase) {
      setEditorText(selectedUseCase.featureText);
    }
  }, [selectedUseCase?.relativePath]);

  const selectCapability = (capability: DocNode) => {
    setActiveCapabilityId(capability.id);
    setExpandedNodeIds((current) => {
      const next = new Set(current);
      next.add(capability.id);
      return next;
    });
    const firstUseCase = firstUseCaseInNode(capability);
    if (firstUseCase) {
      selectUseCase(firstUseCase, panelMode);
    }
  };

  const selectUseCase = (useCase: UseCaseDetails, mode: PanelMode) => {
    setSelectedUseCasePath(useCase.relativePath);
    setPanelMode(mode);
    setMockPullRequest(null);
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

  const toggleScenarios = (relativePath: string) => {
    setExpandedScenarioPaths((current) => {
      const next = new Set(current);
      if (next.has(relativePath)) {
        next.delete(relativePath);
      } else {
        next.add(relativePath);
      }
      return next;
    });
  };

  const submitEpic = async () => {
    if (!selectedUseCase) {
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      const response = await requestJson<MockPullRequestResponse>('/api/capabilities/mock-pr/submit-epic', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          capabilityId: selectedUseCase.capabilityId,
          activityId: selectedUseCase.activityId,
          useCaseId: selectedUseCase.useCaseId,
          relativePath: selectedUseCase.relativePath,
          submittedFeature: editorText,
        }),
      });
      setMockPullRequest(response);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setSubmitting(false);
    }
  };

  const submitScenarioDeletion = async () => {
    if (!pendingDeletion) {
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      const response = await requestJson<MockPullRequestResponse>('/api/capabilities/mock-pr/delete-scenario', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          capabilityId: pendingDeletion.useCase.capabilityId,
          activityId: pendingDeletion.useCase.activityId,
          useCaseId: pendingDeletion.useCase.useCaseId,
          relativePath: pendingDeletion.useCase.relativePath,
          scenarioId: pendingDeletion.scenario.id,
          scenarioName: pendingDeletion.scenario.name,
          scenarioText: pendingDeletion.scenario.text,
          epicText: pendingDeletion.epicText,
        }),
      });
      setPendingDeletion(null);
      setMockPullRequest(response);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setSubmitting(false);
    }
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
        <button type="button" className="primary-button" onClick={() => setNewUseCaseOpen(true)}>
          New Use Case
        </button>
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
              selectedUseCasePath={selectedUseCasePath}
              expandedNodeIds={expandedNodeIds}
              expandedScenarioPaths={expandedScenarioPaths}
              onToggleNode={toggleNode}
              onToggleScenarios={toggleScenarios}
              onSelectUseCase={selectUseCase}
              onDeleteScenario={(useCase, scenario) => setPendingDeletion({
                useCase,
                scenario,
                epicText: defaultDeletionEpic(useCase, scenario),
              })}
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
                  <p className="breadcrumb">{breadcrumb(selectedUseCase)}</p>
                  <h2>{selectedUseCase.useCaseId}</h2>
                </div>
                <div className="segmented-control" aria-label="Use-case view mode">
                  <button
                    type="button"
                    className={panelMode === 'submit' ? 'active' : ''}
                    onClick={() => setPanelMode('submit')}
                  >
                    Submit Epic
                  </button>
                  <button
                    type="button"
                    className={panelMode === 'view' ? 'active' : ''}
                    onClick={() => setPanelMode('view')}
                  >
                    View Use Case
                  </button>
                </div>
              </div>

              {error && <div className="inline-error">{error}</div>}

              {panelMode === 'submit' ? (
                <div className="editor-panel">
                  <div className="editor-header">
                    <span>{selectedUseCase.nextEpicName}</span>
                    <button type="button" className="primary-button" disabled={submitting} onClick={submitEpic}>
                      {submitting ? 'Submitting...' : 'Submit Epic'}
                    </button>
                  </div>
                  <GherkinEditor value={editorText} onChange={setEditorText} minRows={22} />
                </div>
              ) : (
                <MarkdownDocument markdown={selectedUseCase.ucMarkdown} />
              )}

              {mockPullRequest && <MockPullRequestPanel response={mockPullRequest} />}
            </>
          ) : (
            <div className="empty-pane">Select a use case.</div>
          )}
        </section>
      </main>

      {pendingDeletion && (
        <Dialog title="Submit Epic for Scenario Deletion" onClose={() => setPendingDeletion(null)}>
          <div className="dialog-summary">
            <span>{pendingDeletion.useCase.useCaseId}</span>
            <strong>{pendingDeletion.scenario.name}</strong>
          </div>
          <GherkinEditor
            value={pendingDeletion.epicText}
            onChange={(value) => setPendingDeletion({ ...pendingDeletion, epicText: value })}
            minRows={12}
          />
          <div className="dialog-actions">
            <button type="button" className="secondary-button" onClick={() => setPendingDeletion(null)}>
              Cancel
            </button>
            <button
              type="button"
              className="danger-button"
              disabled={submitting}
              onClick={submitScenarioDeletion}
            >
              {submitting ? 'Submitting...' : 'Submit Scenario Deletion'}
            </button>
          </div>
        </Dialog>
      )}

      {newUseCaseOpen && tree && (
        <NewUseCaseDialog
          tree={tree}
          onClose={() => setNewUseCaseOpen(false)}
          onMockPullRequest={(response) => {
            setMockPullRequest(response);
            setNewUseCaseOpen(false);
          }}
        />
      )}
    </Shell>
  );
}

function Shell({ children }: { children: React.ReactNode }) {
  return <div className="capabilities-app">{children}</div>;
}

function TreeNodeView({
  node,
  depth,
  selectedUseCasePath,
  expandedNodeIds,
  expandedScenarioPaths,
  onToggleNode,
  onToggleScenarios,
  onSelectUseCase,
  onDeleteScenario,
}: {
  node: DocNode;
  depth: number;
  selectedUseCasePath: string;
  expandedNodeIds: Set<string>;
  expandedScenarioPaths: Set<string>;
  onToggleNode: (nodeId: string) => void;
  onToggleScenarios: (relativePath: string) => void;
  onSelectUseCase: (useCase: UseCaseDetails, mode: PanelMode) => void;
  onDeleteScenario: (useCase: UseCaseDetails, scenario: ScenarioBlock) => void;
}) {
  const hasChildren = node.children.length > 0;
  const isExpanded = expandedNodeIds.has(node.id);
  const useCase = node.useCase;
  const isSelected = useCase?.relativePath === selectedUseCasePath;
  const scenariosExpanded = useCase ? expandedScenarioPaths.has(useCase.relativePath) : false;

  return (
    <div className="tree-node">
      <div className={isSelected ? 'tree-row selected' : `tree-row ${node.type}`} style={{ paddingLeft: depth * 16 }}>
        {useCase ? (
          <button
            type="button"
            className="icon-button"
            aria-label={`Toggle scenarios for ${useCase.useCaseId}`}
            onClick={() => onToggleScenarios(useCase.relativePath)}
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
              onSelectUseCase(useCase, 'submit');
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
            <button type="button" onClick={() => onSelectUseCase(useCase, 'submit')}>Submit Epic</button>
            <button type="button" onClick={() => onSelectUseCase(useCase, 'view')}>View Use Case</button>
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
                <button
                  type="button"
                  className="delete-scenario-button"
                  aria-label={`Delete ${scenario.name}`}
                  onClick={() => onDeleteScenario(useCase, scenario)}
                >
                  X
                </button>
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
          selectedUseCasePath={selectedUseCasePath}
          expandedNodeIds={expandedNodeIds}
          expandedScenarioPaths={expandedScenarioPaths}
          onToggleNode={onToggleNode}
          onToggleScenarios={onToggleScenarios}
          onSelectUseCase={onSelectUseCase}
          onDeleteScenario={onDeleteScenario}
        />
      ))}
    </div>
  );
}

function GherkinEditor({
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

function MockPullRequestPanel({ response }: { response: MockPullRequestResponse }) {
  return (
    <section className="mock-pr-panel" aria-label="Mock pull request">
      <div className="mock-pr-header">
        <div>
          <p className="eyebrow">{response.id}</p>
          <h3>{response.title}</h3>
        </div>
        <span className="status-pill">{response.status}</span>
      </div>
      <dl className="mock-pr-meta">
        <div><dt>Branch</dt><dd>{response.branch}</dd></div>
        <div><dt>Created</dt><dd>{response.createdAt}</dd></div>
      </dl>
      <div className="changed-files">
        {response.changedFiles.map((file) => (
          <details key={file.path}>
            <summary>{file.path}</summary>
            <pre>{file.content}</pre>
          </details>
        ))}
      </div>
    </section>
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

function NewUseCaseDialog({ tree, onClose, onMockPullRequest }: NewUseCaseDialogProps) {
  const capabilityOptions = tree.root.children.filter((node) => node.type === 'capability').map((node) => node.name);
  const [capabilityId, setCapabilityId] = useState(capabilityOptions[0] ?? '');
  const [activityPath, setActivityPath] = useState(firstActivityPath(tree, capabilityOptions[0]) ?? '');
  const [useCaseId, setUseCaseId] = useState('new-use-case');
  const [featureText, setFeatureText] = useState(defaultNewUseCaseFeature('new-use-case'));
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const activityOptions = useMemo(() => activityPathsForCapability(tree, capabilityId), [tree, capabilityId]);

  const submit = async () => {
    setSubmitting(true);
    setError('');
    try {
      const response = await requestJson<MockPullRequestResponse>('/api/capabilities/mock-pr/create-use-case', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          capabilityId,
          activityPath,
          useCaseId,
          featureText,
        }),
      });
      onMockPullRequest(response);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog title="New Use Case" onClose={onClose}>
      <div className="new-use-case-form">
        <label>
          <span>Capability</span>
          <input
            list="capability-options"
            value={capabilityId}
            onChange={(event) => {
              const value = toSlug(event.currentTarget.value);
              setCapabilityId(value);
              setActivityPath(firstActivityPath(tree, value) ?? activityPath);
            }}
          />
          <datalist id="capability-options">
            {capabilityOptions.map((option) => <option key={option} value={option} />)}
          </datalist>
        </label>

        <label>
          <span>Activity path</span>
          <input
            list="activity-options"
            value={activityPath}
            onChange={(event) => setActivityPath(toPath(event.currentTarget.value))}
          />
          <datalist id="activity-options">
            {activityOptions.map((option) => <option key={option} value={option} />)}
          </datalist>
        </label>

        <label>
          <span>Use case id</span>
          <input
            value={useCaseId}
            onChange={(event) => setUseCaseId(toSlug(event.currentTarget.value))}
          />
        </label>
      </div>

      {error && <div className="inline-error">{error}</div>}

      <GherkinEditor value={featureText} onChange={setFeatureText} minRows={14} />

      <div className="dialog-actions">
        <button type="button" className="secondary-button" onClick={onClose}>Cancel</button>
        <button type="button" className="primary-button" disabled={submitting} onClick={submit}>
          {submitting ? 'Submitting...' : 'Submit Epic'}
        </button>
      </div>
    </Dialog>
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

function defaultDeletionEpic(useCase: UseCaseDetails, scenario: ScenarioBlock) {
  return `Feature: ${useCase.useCaseId}

  Scenario: Delete ${scenario.name}
    Given scenario "${scenario.name}" exists in use case "${useCase.useCaseId}"
    When the scenario deletion epic is submitted
    Then scenario "${scenario.name}" is removed from uc.feature
`;
}

function defaultNewUseCaseFeature(useCaseId: string) {
  return `Feature: ${useCaseId}

  Scenario: Primary flow
    Given the primary actor has a goal
    When the actor completes the use case
    Then the system records the expected outcome
`;
}

function firstActivityPath(tree: CapabilityTreeResponse, capabilityId?: string) {
  return activityPathsForCapability(tree, capabilityId ?? '')[0] ?? '';
}

function activityPathsForCapability(tree: CapabilityTreeResponse, capabilityId: string) {
  const capability = tree.root.children.find((node) => node.name === capabilityId);
  if (!capability) {
    return [];
  }
  const paths: string[] = [];
  const visit = (node: DocNode) => {
    if (node.type === 'activity') {
      const prefix = `${capability.name}/activities/`;
      paths.push(node.relativePath.startsWith(prefix) ? node.relativePath.slice(prefix.length) : node.name);
    }
    node.children.forEach(visit);
  };
  visit(capability);
  return paths;
}

function breadcrumb(useCase: UseCaseDetails) {
  return `${useCase.capabilityId} / ${useCase.activityId} / ${useCase.useCaseId}`;
}

function toSlug(value: string) {
  return value.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-+|-+$)/g, '');
}

function toPath(value: string) {
  return value
    .split('/')
    .map((segment) => toSlug(segment))
    .filter(Boolean)
    .join('/');
}

function errorMessage(caught: unknown) {
  return caught instanceof Error ? caught.message : 'Unexpected error';
}
