package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.BasicCurrencyTransitions;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;

/**
 * 작업 내에서 증명된 상태 동치류에 정수 식별자를 부여한다.
 *
 * <p>기존 계약: Job-local class identities. An integer identifies a proven class, never an incoming
 * item law.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "interned-basic-transition-classes-v1"} ({@code String}).
 *   <li>{@code classes}: 동치류목록 ({@code BasicTransitionClasses}).
 *   <li>{@code storage}: 저장소 ({@code Storage}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code BasicClassIndex(classes)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code classes}(동치류목록).
 *   <li>{@code BasicClassIndex(classes, storage)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code classes}(동치류목록),
 *       {@code storage}(저장소).
 *   <li>{@code intern(actual)}: 동등한 기존 값을 찾아 공유하고 없으면 등록한다. 입력: {@code actual}(실제값). 반환: {@code
 *       int}.
 *   <li>{@code intern(key, witness)}: 동등한 기존 값을 찾아 공유하고 없으면 등록한다. 입력: {@code key}(키), {@code
 *       witness}(증명상태). 반환: {@code int}.
 *   <li>{@code size()}: 보유 항목의 개수를 조회한다. 반환: {@code int}.
 *   <li>{@code residentEntries()}: 메모리상주·항목목록 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
 *   <li>{@code profileMetrics()}: 특성·측정값 값을 조회·구성한다. 반환: {@code
 *       BasicTransitionClasses.ProfileMetrics}.
 *   <li>{@code profileRetainedRows()}: 특성·보유·행목록 값을 조회·구성한다. 반환: {@code int}.
 *   <li>{@code classOf(actual)}: 동치류·Of 처리에서 {@code storage.find}, {@code classes.key}을(를) 사용한다.
 *       입력: {@code actual}(실제값). 반환: {@code int}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code key(id)}: 키 처리에서 {@code storage.entry}을(를) 사용한다. 입력: {@code id}(식별자). 반환: {@code
 *       BasicTransitionClasses.Key}.
 *   <li>{@code witness(id)}: 증명상태 처리에서 {@code storage.entry}을(를) 사용한다. 입력: {@code id}(식별자). 반환:
 *       {@code BasicCurrencyState}.
 *   <li>{@code replay(source, actual, action)}: replay 처리에서 {@code classes.replay}을(를) 사용한다. 입력:
 *       {@code source}(출처), {@code actual}(실제값), {@code action}(행동). 반환: {@code
 *       BasicCurrencyTransitions.PrimitiveCursor}.
 *   <li>{@code cursor(source, action)}: 진행커서의 입력에 따른 분기·결과를 처리한다. 입력: {@code source}(출처), {@code
 *       action}(행동). 반환: {@code Cursor}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Entry}: {@code BasicClassIndex}에서 사용하는 항목의 불변 값이다.
 *   <li>{@code Storage}: {@code BasicClassIndex} 내부의 저장소 처리를 맡는다.
 *   <li>{@code MemoryStorage}: {@code BasicClassIndex} 내부의 메모리·저장소 처리를 맡는다.
 *   <li>{@code Cursor}: {@code BasicClassIndex} 내부의 진행커서 처리를 맡는다.
 * </ul>
 */
public final class BasicClassIndex implements AutoCloseable {
  public static final String VERSION = "interned-basic-transition-classes-v1";

  /**
   * {@code BasicClassIndex}에서 사용하는 항목의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code key}: 키 ({@code BasicTransitionClasses.Key}).
   *   <li>{@code witness}: 증명상태 ({@code BasicCurrencyState}).
   * </ul>
   */
  public record Entry(BasicTransitionClasses.Key key, BasicCurrencyState witness) {}

  /**
   * {@code BasicClassIndex} 내부의 저장소 처리를 맡는다.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code find(key)}: 조건에 맞는 항목을 찾는다. 입력: {@code key}(키). 반환: {@code Integer}.
   *   <li>{@code add(key, witness)}: 값 또는 항목을 더한다. 입력: {@code key}(키), {@code witness}(증명상태). 반환:
   *       {@code int}.
   *   <li>{@code entry(id)}: 항목의 실행 계약을 정의한다. 입력: {@code id}(식별자). 반환: {@code Entry}.
   *   <li>{@code size()}: 보유 항목의 개수를 조회한다. 반환: {@code int}.
   *   <li>{@code residentEntries()}: 메모리상주·항목목록 값을 조회·구성한다. 반환: {@code long}.
   *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
   * </ul>
   */
  public interface Storage extends AutoCloseable {
    Integer find(BasicTransitionClasses.Key key);

    int add(BasicTransitionClasses.Key key, BasicCurrencyState witness);

    Entry entry(int id);

    int size();

    long residentEntries();

    @Override
    void close();
  }

  /**
   * {@code BasicClassIndex} 내부의 메모리·저장소 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code identities}: 식별자목록 ({@code Map<BasicTransitionClasses.Key, Integer>}).
   *   <li>{@code entries}: 항목목록 ({@code List<Entry>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code find(key)}: 조건에 맞는 항목을 찾는다. 입력: {@code key}(키). 반환: {@code Integer}.
   *   <li>{@code add(key, witness)}: 값 또는 항목을 더한다. 입력: {@code key}(키), {@code witness}(증명상태). 반환:
   *       {@code int}.
   *   <li>{@code entry(id)}: 항목 처리에서 {@code entries.get}을(를) 사용한다. 입력: {@code id}(식별자). 반환: {@code
   *       Entry}.
   *   <li>{@code size()}: 보유 항목의 개수를 조회한다. 반환: {@code int}.
   *   <li>{@code residentEntries()}: 메모리상주·항목목록 값을 조회·구성한다. 반환: {@code long}.
   *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
   * </ul>
   */
  private static final class MemoryStorage implements Storage {
    private final Map<BasicTransitionClasses.Key, Integer> identities = new HashMap<>();
    private final List<Entry> entries = new ArrayList<>();

    public Integer find(BasicTransitionClasses.Key key) {
      return identities.get(key);
    }

    public int add(BasicTransitionClasses.Key key, BasicCurrencyState witness) {
      int id = entries.size();
      identities.put(key, id);
      entries.add(new Entry(key, witness));
      return id;
    }

    public Entry entry(int id) {
      return entries.get(id);
    }

    public int size() {
      return entries.size();
    }

    public long residentEntries() {
      return entries.size();
    }

    public void close() {
      identities.clear();
      entries.clear();
    }
  }

  private final BasicTransitionClasses classes;
  private final Storage storage;

  public BasicClassIndex(BasicTransitionClasses classes) {
    this(classes, new MemoryStorage());
  }

  public BasicClassIndex(BasicTransitionClasses classes, Storage storage) {
    this.classes = Objects.requireNonNull(classes);
    this.storage = Objects.requireNonNull(storage);
  }

  public int intern(BasicCurrencyState actual) {
    return intern(classes.key(actual), actual);
  }

  private int intern(BasicTransitionClasses.Key key, BasicCurrencyState witness) {
    var existing = storage.find(key);
    if (existing != null) return existing;
    return storage.add(key, witness);
  }

  public int size() {
    return storage.size();
  }

  public long residentEntries() {
    return storage.residentEntries();
  }

  public void close() {
    storage.close();
  }

  public BasicTransitionClasses.ProfileMetrics profileMetrics() {
    return classes.profileMetrics();
  }

  public int profileRetainedRows() {
    return classes.profileRetainedRows();
  }

  public int classOf(BasicCurrencyState actual) {
    var id = storage.find(classes.key(actual));
    if (id == null) throw new IllegalArgumentException("Item has no registered transition class");
    return id;
  }

  public BasicTransitionClasses.Key key(int id) {
    return storage.entry(id).key();
  }

  public BasicCurrencyState witness(int id) {
    return storage.entry(id).witness();
  }

  /** Validate full equality of the class key; hash equality alone never admits an input. */
  public BasicCurrencyTransitions.PrimitiveCursor replay(
      int source, BasicCurrencyState actual, WorkbenchCurrency action) {
    return classes.replay(key(source), actual, action);
  }

  public Cursor cursor(int source, WorkbenchCurrency action) {
    return new Cursor(source, action);
  }

  /**
   * {@code BasicClassIndex} 내부의 진행커서 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code projection}: 투영 ({@code BasicTransitionClasses.IndexedCursor}).
   *   <li>{@code assumptions}: 확률·수치 계산에 사용한 명시적 가정 ({@code List<WorkbenchSimulator.Assumption>}).
   *   <li>{@code projected}: 투영된 ({@code BasicTransitionClasses.Kernel}).
   *   <li>{@code pending}: 대기 ({@code Iterator<Map.Entry<BasicTransitionClasses.Key, Fraction>>}).
   *   <li>{@code outcomes}: 결과목록 ({@code List<ExactMethodRunner.Outcome<Integer>>}).
   *   <li>{@code result}: 결과 ({@code ExactMethodRunner.Kernel<Integer>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Cursor(source, action)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code source}(출처), {@code
   *       action}(행동).
   *   <li>{@code assumptions()}: {@code assumptions} 값을 조회한다. 반환: {@code
   *       List<WorkbenchSimulator.Assumption>}.
   *   <li>{@code complete()}: 완료 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
   *   <li>{@code result()}: 최종 또는 현재 계산 결과를 구성한다. 반환: {@code ExactMethodRunner.Kernel<Integer>}.
   *       사용하는 예외: {@code IllegalStateException}.
   * </ul>
   */
  public final class Cursor {
    private BasicTransitionClasses.IndexedCursor projection;
    private final List<WorkbenchSimulator.Assumption> assumptions;
    private BasicTransitionClasses.Kernel projected;
    private Iterator<Map.Entry<BasicTransitionClasses.Key, Fraction>> pending;
    private final List<ExactMethodRunner.Outcome<Integer>> outcomes = new ArrayList<>();
    private ExactMethodRunner.Kernel<Integer> result;

    private Cursor(int source, WorkbenchCurrency action) {
      projection = classes.indexedCursor(witness(source), action);
      assumptions = projection.assumptions();
    }

    public List<WorkbenchSimulator.Assumption> assumptions() {
      return assumptions;
    }

    public boolean complete() {
      return result != null;
    }

    /** Each step advances one candidate band/witness, or interns one completed outcome. */
    public void step() {
      if (complete()) return;
      if (projected == null) {
        if (!projection.complete()) {
          projection.step();
          return;
        }
        projected = projection.result();
        projection = null;
        pending = projected.outcomes().entrySet().iterator();
      }
      if (pending.hasNext()) {
        var entry = pending.next();
        int target = intern(entry.getKey(), projected.witnesses().get(entry.getKey()));
        outcomes.add(new ExactMethodRunner.Outcome<>(target, entry.getValue()));
        return;
      }
      result =
          new ExactMethodRunner.Kernel<>(outcomes, projected.unresolved(), projected.unavailable());
      projected = null;
      pending = null;
      outcomes.clear();
    }

    public ExactMethodRunner.Kernel<Integer> result() {
      if (!complete()) throw new IllegalStateException("Partial indexed kernel");
      return result;
    }
  }
}
