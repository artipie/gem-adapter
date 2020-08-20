require 'java'
require 'builder'
require 'rubygems/indexer.rb'
require 'securerandom'
require 'AstoIndexer.rb'
require 'fileutils'
# @todo #32:120min Gem submission implementation.
#  The implementation must receive the .gem file, unzip it, and update specs files. As a result,
#  files become available for downloading.
class SubmitGem
  java_import com.artipie.http.async.AsyncResponse
  java_import com.artipie.http.rs.RsWithStatus
  java_import com.artipie.http.rs.RsStatus
  java_import com.artipie.asto.rx.RxCopy
  java_import com.artipie.asto.rx.RxStorageWrapper
  java_import com.artipie.asto.fs.FileStorage
  java_import com.artipie.asto.fs.RxFile
  java_import com.artipie.asto.Key
  java_import Java::io.reactivex.Flowable
  java_import Java::io.reactivex.Single
  java_import Java::io.reactivex.Completable
  java_import java.util.concurrent.TimeUnit
  java_import java.nio.file.Paths
  java_import java.nio.file.Files
  java_import java.util.ArrayList
  java_import java.util.HashSet
  java_import org.slf4j.LoggerFactory
  include com.artipie.http.Slice

  @@log = LoggerFactory::getLogger("com.artipie.gem.SubmitGem")

  def initialize(storage)
    @storage = storage
    @idx = "temp-gem-index"
    @gems = File.join(@idx, "gems")
    # Was index created before?
    idx_existed = File.exists?(@idx)
    Dir.mkdir(@idx) unless idx_existed
    Dir.mkdir(@gems) unless File.exists?(@gems)
    @indexer = AstoIndexer.new(storage ,@idx, { build_modern: true })
    @indexer.generate_index unless idx_existed
    @rx_idx_local = RxStorageWrapper.new(FileStorage.new(Paths::get(@idx)))
    @rx_storage = RxStorageWrapper.new(@storage)
  end

  def response(line, headers, body)
    @@log.debug("Requested #{line}")
    rnd_gem = SecureRandom.hex(32) + ".gem"
    rnd_path = File.join(@gems, rnd_gem)
    jrnd_path = Paths::get(@gems, rnd_gem)
    AsyncResponse.new(
        RxFile.new(jrnd_path).save(Flowable::fromPublisher(body))
            .and_then(Completable::from_action{
              spec = Gem::Package.new(rnd_path).spec
              real_path = File.join(@gems, "#{spec.name}-#{spec.version}.gem")
              FileUtils.mv(rnd_path, real_path)
            })
            .and_then(self.sync(@rx_storage, @rx_idx_local))
            .and_then(Completable::from_action { @indexer.update_index })
            .and_then(self.sync(@rx_idx_local, @rx_storage))
            .and_then(Single::just(RsWithStatus.new(RsStatus::OK)))
    )
  end

  #
  # from - RxStorage to sync from
  # to - RxStorage to sync with
  def sync(from, to)
    # Specs files to copy if exists
    specs = from.list(Key::ROOT).flatMapPublisher { |keys|
      keys.retainAll(
          HashSet.new(
              [
                  Key::From.new("latest_specs.4.8"),
                  Key::From.new("latest_specs.4.8.gz"),
                  Key::From.new("prerelease_specs.4.8"),
                  Key::From.new("prerelease_specs.4.8.gz"),
                  Key::From.new("specs.4.8"),
                  Key::From.new("specs.4.8.gz")
              ]
          )
      )
      Flowable::from_iterable(keys)
    }
    # Non-specs files, copy if not exists
    specs
        .mergeWith(diff_keys("quick", from, to))
        .mergeWith(diff_keys("gems", from, to))
        .to_list
        .flatMapCompletable do |keys|
      @@log.debug("Copy {}", keys)
      RxCopy.new(from, keys).copy(to)
    end
  end

  # Find the storage differences in a particular directory
  # dir - the directory in which to find the difference
  # from - RxStorage to sync from
  # to - RxStorage to sync with
  private def diff_keys(dir, from, to)
    from.list(Key::From.new(dir))
        .zipWith(
            to.list(Key::From.new(dir)),
            -> src, dest {
              src.removeAll(dest)
              src
            }
        )
        .flatMapPublisher { |keys| Flowable::from_iterable(keys) }
  end
end